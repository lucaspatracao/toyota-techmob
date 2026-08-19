"""
TechMob 4.0 - Módulo de Ciência de Dados
Cálculo do OEE (Overall Equipment Effectiveness) a partir dos CSVs
gerados pelo Node-RED (dados brutos da Bancada Smart 4.0).

Fluxo (conforme Documento do Projeto, seção 5.3):
  1. Lê o(s) CSV(s) de produção (pandas).
  2. Calcula estatística descritiva por período (dia/turno).
  3. Calcula Disponibilidade, Performance, Qualidade e OEE.
  4. Calcula média móvel do OEE (tendência).
  5. Grava os indicadores calculados no PostgreSQL (Supabase),
     tabela `techmob.indicador_oee`.

Uso:
    python oee_calculator.py --csv dados_producao.csv --maquina-id 1

Formato esperado do CSV (dados_producao.csv), conforme o documento:
    timestamp,bancada_id,status_operacional,pecas_boas,pecas_defeituosas,tempo_ciclo_segundos
    2026-08-17T14:32:10Z,BANCADA_SMART_01,EM_PRODUCAO,128,3,12.5
"""

import argparse
import logging
import os
from dataclasses import dataclass
from datetime import datetime, timezone

import numpy as np
import pandas as pd

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("oee_calculator")


# ---------------------------------------------------------------------------
# Configuração dos parâmetros do OEE
# ---------------------------------------------------------------------------
# Estes valores dependem da especificação física/operacional da Bancada Smart 4.0
# e devem ser ajustados pela equipe (ou movidos para a tabela `maquina` no banco,
# como colunas `tempo_planejado_segundos` e `capacidade_teorica_pecas_seg`).
@dataclass
class ConfiguracaoOEE:
    tempo_planejado_segundos: float = 8 * 3600   # ex.: turno de 8h
    capacidade_teorica_pecas_seg: float = 1 / 10  # ex.: 1 peça a cada 10s (ajustar)


# ---------------------------------------------------------------------------
# Leitura e validação
# ---------------------------------------------------------------------------
COLUNAS_ESPERADAS = [
    "timestamp",
    "bancada_id",
    "status_operacional",
    "pecas_boas",
    "pecas_defeituosas",
    "tempo_ciclo_segundos",
]


def ler_csv_producao(caminho_csv: str) -> pd.DataFrame:
    """Lê o CSV de produção gerado pelo Node-RED e valida as colunas."""
    if not os.path.exists(caminho_csv):
        raise FileNotFoundError(f"CSV não encontrado: {caminho_csv}")

    df = pd.read_csv(caminho_csv)

    faltando = set(COLUNAS_ESPERADAS) - set(df.columns)
    if faltando:
        raise ValueError(f"Colunas ausentes no CSV: {faltando}")

    df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
    df = df.sort_values("timestamp").reset_index(drop=True)
    return df


def filtrar_por_maquina(df: pd.DataFrame, bancada_id: str | None) -> pd.DataFrame:
    if bancada_id is None:
        return df
    return df[df["bancada_id"] == bancada_id].reset_index(drop=True)


# ---------------------------------------------------------------------------
# Estatística descritiva
# ---------------------------------------------------------------------------
def estatisticas_descritivas(df: pd.DataFrame) -> dict:
    """Estatísticas descritivas do turno/período: médias, desvio padrão, min/max."""
    return {
        "tempo_ciclo_medio": float(df["tempo_ciclo_segundos"].mean()),
        "tempo_ciclo_desvio_padrao": float(df["tempo_ciclo_segundos"].std(ddof=0)),
        "tempo_ciclo_min": float(df["tempo_ciclo_segundos"].min()),
        "tempo_ciclo_max": float(df["tempo_ciclo_segundos"].max()),
        "total_pecas_boas": int(df["pecas_boas"].sum()),
        "total_pecas_defeituosas": int(df["pecas_defeituosas"].sum()),
    }


# ---------------------------------------------------------------------------
# Cálculo do OEE (fórmulas da seção 5.3 do documento)
# ---------------------------------------------------------------------------
def calcular_oee(df: pd.DataFrame, config: ConfiguracaoOEE) -> dict:
    total_pecas_boas = int(df["pecas_boas"].sum())
    total_pecas_defeituosas = int(df["pecas_defeituosas"].sum())
    quantidade_total_produzida = total_pecas_boas + total_pecas_defeituosas

    # Tempo operacional real: soma dos tempos de ciclo apenas quando em produção
    em_producao = df[df["status_operacional"] == "EM_PRODUCAO"]
    tempo_operacional_real = float(em_producao["tempo_ciclo_segundos"].sum())

    if config.tempo_planejado_segundos <= 0:
        raise ValueError("tempo_planejado_segundos deve ser maior que zero")

    disponibilidade = (tempo_operacional_real / config.tempo_planejado_segundos) * 100

    if tempo_operacional_real > 0 and config.capacidade_teorica_pecas_seg > 0:
        performance = (
            quantidade_total_produzida
            / (tempo_operacional_real * config.capacidade_teorica_pecas_seg)
        ) * 100
    else:
        performance = 0.0

    if quantidade_total_produzida > 0:
        qualidade = (total_pecas_boas / quantidade_total_produzida) * 100
    else:
        qualidade = 0.0

    # OEE = Disponibilidade x Performance x Qualidade (as três em fração, resultado em %)
    oee = (disponibilidade / 100) * (performance / 100) * (qualidade / 100) * 100

    return {
        "disponibilidade": round(disponibilidade, 2),
        "performance": round(performance, 2),
        "qualidade": round(qualidade, 2),
        "oee": round(oee, 2),
        "tempo_operacional_real_segundos": round(tempo_operacional_real, 2),
        "quantidade_total_produzida": quantidade_total_produzida,
    }


def media_movel_oee(df: pd.DataFrame, config: ConfiguracaoOEE, janela: str = "1h") -> pd.DataFrame:
    """
    Calcula o OEE por janela de tempo (ex.: por hora) e sua média móvel,
    para identificar tendências/quedas de eficiência ao longo do turno.
    """
    df_indexed = df.set_index("timestamp")
    resultados = []

    for periodo, grupo in df_indexed.resample(janela):
        if grupo.empty:
            continue
        oee_periodo = calcular_oee(grupo.reset_index(), config)
        oee_periodo["periodo"] = periodo
        resultados.append(oee_periodo)

    df_oee = pd.DataFrame(resultados)
    if not df_oee.empty:
        df_oee["oee_media_movel"] = df_oee["oee"].rolling(window=3, min_periods=1).mean().round(2)
    return df_oee


# ---------------------------------------------------------------------------
# Persistência no PostgreSQL (Supabase) - tabela techmob.indicador_oee
# ---------------------------------------------------------------------------
def gravar_indicador_postgres(maquina_id: int, indicadores: dict, calculado_em: datetime | None = None):
    """
    Grava o indicador calculado na tabela `techmob.indicador_oee`.
    Requer as variáveis de ambiente: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD.

    Import de sqlalchemy/psycopg2 feito localmente para permitir rodar o script
    apenas em modo de cálculo (sem gravar no banco) quando essas libs/env vars
    não estiverem disponíveis.
    """
    from sqlalchemy import create_engine, text

    host = os.environ["DB_HOST"]
    port = os.environ.get("DB_PORT", "5432")
    dbname = os.environ["DB_NAME"]
    user = os.environ["DB_USER"]
    password = os.environ["DB_PASSWORD"]

    url = f"postgresql+psycopg2://{user}:{password}@{host}:{port}/{dbname}"
    engine = create_engine(url, connect_args={"sslmode": "require"})

    calculado_em = calculado_em or datetime.now(timezone.utc)

    query = text(
        """
        INSERT INTO techmob.indicador_oee
            (maquina_id, disponibilidade, performance, qualidade, oee, calculado_em)
        VALUES
            (:maquina_id, :disponibilidade, :performance, :qualidade, :oee, :calculado_em)
        """
    )

    with engine.begin() as conn:
        conn.execute(
            query,
            {
                "maquina_id": maquina_id,
                "disponibilidade": indicadores["disponibilidade"],
                "performance": indicadores["performance"],
                "qualidade": indicadores["qualidade"],
                "oee": indicadores["oee"],
                "calculado_em": calculado_em,
            },
        )
    logger.info("Indicador OEE gravado no Supabase para maquina_id=%s", maquina_id)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(description="Cálculo do OEE - TechMob 4.0")
    parser.add_argument("--csv", required=True, help="Caminho do CSV de produção")
    parser.add_argument("--bancada-id", default=None, help="Filtrar por bancada_id (ex.: BANCADA_SMART_01)")
    parser.add_argument("--maquina-id", type=int, default=None, help="ID da máquina no banco (para gravação)")
    parser.add_argument("--gravar-banco", action="store_true", help="Gravar o resultado no Supabase")
    parser.add_argument("--tendencia", action="store_true", help="Calcular também a série de OEE por hora + média móvel")
    args = parser.parse_args()

    config = ConfiguracaoOEE()

    df = ler_csv_producao(args.csv)
    df = filtrar_por_maquina(df, args.bancada_id)

    if df.empty:
        logger.warning("Nenhum dado encontrado após os filtros aplicados.")
        return

    stats = estatisticas_descritivas(df)
    logger.info("Estatísticas descritivas: %s", stats)

    indicadores = calcular_oee(df, config)
    logger.info("Indicadores OEE: %s", indicadores)

    if args.tendencia:
        df_tendencia = media_movel_oee(df, config)
        logger.info("Série de OEE por hora + média móvel:\n%s", df_tendencia)

    if args.gravar_banco:
        if args.maquina_id is None:
            raise ValueError("--maquina-id é obrigatório ao usar --gravar-banco")
        gravar_indicador_postgres(args.maquina_id, indicadores)


if __name__ == "__main__":
    main()

"""
TechMob 4.0 - Módulo de Ciência de Dados
Cálculo do OEE (Overall Equipment Effectiveness) a partir dos CSVs
gerados pelo Node-RED (dados brutos da Bancada Smart 4.0).

Fluxo:
  1. Lê os CSVs de produção (pandas).
  2. Calcula as métricas de Disponibilidade, Performance e Qualidade.
  3. Calcula o OEE por período.
  4. Grava os resultados na tabela `techmob.indicador_oee`.
  5. Caso `--watch-dir` seja informado, varre o diretório em loop contínuo
     com `time.sleep` para processar novos CSVs automaticamente.

Uso:
    python oee_calculator.py --csv dados_producao.csv --maquina-id 1 --gravar-banco
    python oee_calculator.py --watch-dir ./dados --maquina-id 1 --gravar-banco --intervalo 30
"""

import argparse
import logging
import os
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import pandas as pd

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("oee_calculator")


@dataclass
class ConfiguracaoOEE:
    tempo_planejado_segundos: float = 8 * 3600
    capacidade_teorica_pecas_seg: float = 1 / 10


COLUNAS_ESPERADAS = [
    "timestamp",
    "bancada_id",
    "status_operacional",
    "pecas_boas",
    "pecas_defeituosas",
    "tempo_ciclo_segundos",
]


def obter_url_mysql() -> str:
    host = os.getenv("DB_HOST", "localhost")
    port = os.getenv("DB_PORT", "3306")
    dbname = os.getenv("DB_NAME", "techmob")
    user = os.getenv("DB_USER", "root")
    password = os.getenv("DB_PASSWORD", "123456")
    return f"mysql+pymysql://{user}:{password}@{host}:{port}/{dbname}?charset=utf8mb4"


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


def estatisticas_descritivas(df: pd.DataFrame) -> dict:
    return {
        "tempo_ciclo_medio": float(df["tempo_ciclo_segundos"].mean()),
        "tempo_ciclo_desvio_padrao": float(df["tempo_ciclo_segundos"].std(ddof=0)),
        "tempo_ciclo_min": float(df["tempo_ciclo_segundos"].min()),
        "tempo_ciclo_max": float(df["tempo_ciclo_segundos"].max()),
        "total_pecas_boas": int(df["pecas_boas"].sum()),
        "total_pecas_defeituosas": int(df["pecas_defeituosas"].sum()),
    }


def calcular_oee(df: pd.DataFrame, config: ConfiguracaoOEE) -> dict:
    total_pecas_boas = int(df["pecas_boas"].sum())
    total_pecas_defeituosas = int(df["pecas_defeituosas"].sum())
    quantidade_total_produzida = total_pecas_boas + total_pecas_defeituosas

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

    oee = (disponibilidade / 100) * (performance / 100) * (qualidade / 100) * 100

    return {
        "disponibilidade": round(disponibilidade, 2),
        "performance": round(performance, 2),
        "qualidade": round(qualidade, 2),
        "oee": round(oee, 2),
        "tempo_operacional_real_segundos": round(tempo_operacional_real, 2),
        "quantidade_total_produzida": quantidade_total_produzida,
        "pecas_boas": total_pecas_boas,
        "pecas_defeituosas": total_pecas_defeituosas,
    }


def media_movel_oee(df: pd.DataFrame, config: ConfiguracaoOEE, janela: str = "1h") -> pd.DataFrame:
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


def gravar_indicador_mysql(
    maquina_id: int,
    indicadores: dict,
    periodo_inicio: datetime,
    periodo_fim: datetime,
    calculado_em: datetime | None = None,
):
    from sqlalchemy import create_engine, text

    url = obter_url_mysql()
    engine = create_engine(url)
    criado_em = calculado_em or datetime.now(timezone.utc)

    query = text(
        """
        INSERT INTO indicador_oee
            (maquina_id, periodo_inicio, periodo_fim, disponibilidade, performance,
             qualidade, oee, pecas_boas, pecas_defeituosas, criado_em)
        VALUES
            (:maquina_id, :periodo_inicio, :periodo_fim, :disponibilidade, :performance,
             :qualidade, :oee, :pecas_boas, :pecas_defeituosas, :criado_em)
        """
    )

    with engine.begin() as conn:
        conn.execute(
            query,
            {
                "maquina_id": maquina_id,
                "periodo_inicio": periodo_inicio,
                "periodo_fim": periodo_fim,
                "disponibilidade": indicadores["disponibilidade"],
                "performance": indicadores["performance"],
                "qualidade": indicadores["qualidade"],
                "oee": indicadores["oee"],
                "pecas_boas": indicadores["pecas_boas"],
                "pecas_defeituosas": indicadores["pecas_defeituosas"],
                "criado_em": criado_em,
            },
        )

    logger.info("Indicador OEE gravado no MySQL para maquina_id=%s", maquina_id)


def processar_csv(caminho_csv: str, maquina_id: int = 1, bancada_id: str | None = None, gravar_banco: bool = True):
    df = ler_csv_producao(caminho_csv)
    df = filtrar_por_maquina(df, bancada_id)

    if df.empty:
        logger.warning("Nenhum dado encontrado após os filtros aplicados no arquivo %s", caminho_csv)
        return None

    stats = estatisticas_descritivas(df)
    logger.info("Estatísticas do arquivo %s: %s", caminho_csv, stats)

    indicadores = calcular_oee(df, ConfiguracaoOEE())
    logger.info("Indicadores OEE do arquivo %s: %s", caminho_csv, indicadores)

    if gravar_banco:
        gravar_indicador_mysql(
            maquina_id=maquina_id,
            indicadores=indicadores,
            periodo_inicio=df["timestamp"].min().to_pydatetime(),
            periodo_fim=df["timestamp"].max().to_pydatetime(),
        )

    return indicadores


def varrer_diretorio(
    diretorio: str,
    maquina_id: int = 1,
    bancada_id: str | None = "BANCADA_SMART_01",
    gravar_banco: bool = True,
    intervalo_segundos: int = 30,
):
    diretorio_path = Path(diretorio)
    processados = {}

    logger.info("Iniciando varredura contínua em %s a cada %s segundos", diretorio_path, intervalo_segundos)

    while True:
        for arquivo_csv in sorted(diretorio_path.glob("*.csv")):
            chave = arquivo_csv.resolve()
            ultima_modificacao = arquivo_csv.stat().st_mtime
            if processados.get(chave) == ultima_modificacao:
                continue

            logger.info("Processando arquivo: %s", arquivo_csv)
            try:
                processar_csv(
                    caminho_csv=str(arquivo_csv),
                    maquina_id=maquina_id,
                    bancada_id=bancada_id,
                    gravar_banco=gravar_banco,
                )
                processados[chave] = ultima_modificacao
            except Exception:
                logger.exception("Erro ao processar o arquivo %s", arquivo_csv)

        time.sleep(intervalo_segundos)


def main():
    parser = argparse.ArgumentParser(description="Cálculo do OEE - TechMob 4.0")
    parser.add_argument("--csv", help="Caminho do CSV de produção para processamento único")
    parser.add_argument("--watch-dir", help="Diretório que será varrido em loop contínuo para CSVs")
    parser.add_argument("--intervalo", type=int, default=30, help="Intervalo em segundos para varrer o diretório em loop")
    parser.add_argument("--bancada-id", default="BANCADA_SMART_01", help="Filtrar por bancada_id (ex.: BANCADA_SMART_01)")
    parser.add_argument("--maquina-id", type=int, default=1, help="ID da máquina no banco (valor padrão: 1)")
    parser.add_argument("--gravar-banco", action="store_true", help="Gravar o resultado no MySQL")
    parser.add_argument("--tendencia", action="store_true", help="Calcular também a série de OEE por hora + média móvel")
    args = parser.parse_args()

    if args.watch_dir:
        varrer_diretorio(
            diretorio=args.watch_dir,
            maquina_id=args.maquina_id,
            bancada_id=args.bancada_id,
            gravar_banco=args.gravar_banco,
            intervalo_segundos=args.intervalo,
        )
        return

    if not args.csv:
        raise ValueError("Informe --csv ou --watch-dir")

    df = ler_csv_producao(args.csv)
    df = filtrar_por_maquina(df, args.bancada_id)

    if df.empty:
        logger.warning("Nenhum dado encontrado após os filtros aplicados.")
        return

    stats = estatisticas_descritivas(df)
    logger.info("Estatísticas descritivas: %s", stats)

    indicadores = calcular_oee(df, ConfiguracaoOEE())
    logger.info("Indicadores OEE: %s", indicadores)

    if args.tendencia:
        df_tendencia = media_movel_oee(df, ConfiguracaoOEE())
        logger.info("Série de OEE por hora + média móvel:\n%s", df_tendencia)

    if args.gravar_banco:
        gravar_indicador_mysql(
            args.maquina_id,
            indicadores,
            periodo_inicio=df["timestamp"].min().to_pydatetime(),
            periodo_fim=df["timestamp"].max().to_pydatetime(),
        )


if __name__ == "__main__":
    main()

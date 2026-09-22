"""
TechMob 4.0 - Processador OEE com Simulação
Versão adaptada para Windows
"""

import argparse
import logging
import math
import os
import random
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from enum import Enum
from typing import Dict, Optional

import pandas as pd

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
)
logger = logging.getLogger("oee_calculator")


class StatusOperacional(Enum):
    """Status operacionais da máquina."""

    PRODUCAO = "EM_PRODUCAO"
    PARADA = "PARADA_PROGRAMADA"
    FALHA = "FALHA_NAO_PROGRAMADA"
    MANUTENCAO = "MANUTENCAO_PREVENTIVA"


@dataclass
class ConfiguracaoOEE:
    """Configuração dos parâmetros do OEE."""

    tempo_planejado_segundos: float = 8 * 3600
    capacidade_teorica_pecas_seg: float = 1 / 8.5
    nome_maquina: str = "BANCADA_SMART_01"
    tempo_ciclo_base: float = 8.5
    variabilidade_ciclo: float = 0.9
    probabilidade_falha: float = 0.008
    taxa_defeito_base: float = 0.018


class SimuladorBancadaSmart:
    """Simulador realista da Bancada Smart 4.0 para Windows."""

    def __init__(self, config: Optional[ConfiguracaoOEE] = None):
        self.config = config or ConfiguracaoOEE()
        self._estado_atual = StatusOperacional.PRODUCAO
        self._contador_pecas = 0
        self._tempo_restante_falha = 0
        self._tick = 0

    def _trend_value(self, indice: int) -> float:
        """Cria um comportamento realista de linha: arrancada, pico, ajuste e retomada."""
        ciclo = self._tick + indice
        onda_diaria = 0.72 + 0.18 * math.sin(ciclo / 90.0 - 0.9)
        carga_operacional = 0.08 * math.sin(ciclo / 22.0)
        ajuste_manutencao = 0.12 * math.exp(-((ciclo % 210) - 155) ** 2 / 450)
        return max(0.30, min(1.0, onda_diaria + carga_operacional - ajuste_manutencao))

    def gerar_ciclo(self, timestamp: Optional[datetime] = None, indice: int = 0) -> Dict:
        """Gera um ciclo de produção com evolução gradual e plausível."""
        if timestamp is None:
            timestamp = datetime.now(timezone.utc)

        trend = self._trend_value(indice)
        ciclo = self._tick + indice

        if self._estado_atual == StatusOperacional.FALHA:
            self._tempo_restante_falha -= 1
            if self._tempo_restante_falha <= 0:
                self._estado_atual = StatusOperacional.PRODUCAO
                status = StatusOperacional.PRODUCAO
                tempo_ciclo = self.config.tempo_ciclo_base * (0.98 + 0.04 * math.sin(ciclo / 17.0))
            else:
                status = StatusOperacional.FALHA
                tempo_ciclo = 1.0
        elif self._estado_atual == StatusOperacional.PARADA:
            self._tempo_restante_falha -= 1
            if self._tempo_restante_falha <= 0:
                self._estado_atual = StatusOperacional.PRODUCAO
                status = StatusOperacional.PRODUCAO
                tempo_ciclo = self.config.tempo_ciclo_base
            else:
                status = StatusOperacional.PARADA
                tempo_ciclo = 1.0
        else:
            risco_falha = self.config.probabilidade_falha + max(0.0, 0.09 * (0.65 - trend))
            risco_parada = 0.03 + max(0.0, 0.12 * (0.5 - trend))
            decisao = random.random()

            if trend < 0.42 and decisao < risco_parada:
                self._estado_atual = StatusOperacional.PARADA
                self._tempo_restante_falha = random.randint(14, 60)
                status = StatusOperacional.PARADA
                tempo_ciclo = 1.0
            elif trend < 0.6 and decisao < risco_falha:
                self._estado_atual = StatusOperacional.FALHA
                self._tempo_restante_falha = random.randint(8, 32)
                status = StatusOperacional.FALHA
                tempo_ciclo = 1.0
            else:
                status = StatusOperacional.PRODUCAO
                deslocamento = (trend - 0.72) * self.config.variabilidade_ciclo * 1.7
                tempo_ciclo = max(
                    6.5,
                    min(
                        11.5,
                        self.config.tempo_ciclo_base + deslocamento + 0.15 * math.sin(ciclo / 11.0),
                    ),
                )
                self._contador_pecas += 1

        if status == StatusOperacional.PRODUCAO:
            pecas_ciclo = 1 + (1 if trend > 0.74 and random.random() < 0.22 else 0)
            taxa_defeito = max(
                0.008,
                min(
                    0.07,
                    self.config.taxa_defeito_base + (0.05 * (1 - trend)) + 0.005 * math.sin(ciclo / 13.0),
                ),
            )
            pecas_defeituosas = 1 if random.random() < taxa_defeito else 0
            pecas_boas = max(0, pecas_ciclo - pecas_defeituosas)
        else:
            pecas_boas = 0
            pecas_defeituosas = 0
            tempo_ciclo = 1.0

        self._tick += 1

        return {
            'timestamp': timestamp,
            'bancada_id': self.config.nome_maquina,
            'status_operacional': status.value,
            'pecas_boas': int(pecas_boas),
            'pecas_defeituosas': int(pecas_defeituosas),
            'tempo_ciclo_segundos': round(float(tempo_ciclo), 2),
        }

    def gerar_lote(
        self,
        duracao_horas: float = 8,
        passo_segundos: float = 5,
        arquivo_saida: Optional[str] = None,
    ) -> pd.DataFrame:
        """Gera dados em intervalos fixos, mantendo a evolução coerente."""
        if duracao_horas <= 0:
            raise ValueError("duracao_horas deve ser maior que zero")
        if passo_segundos <= 0:
            raise ValueError("passo_segundos deve ser maior que zero")

        dados = []
        tempo_inicio = datetime.now(timezone.utc) - timedelta(hours=duracao_horas)
        tempo_atual = tempo_inicio
        tempo_fim = tempo_inicio + timedelta(hours=duracao_horas)

        logger.info(f"Iniciando simulação de {duracao_horas} horas (passo={passo_segundos}s)...")

        indice = 0
        while tempo_atual < tempo_fim:
            ciclo = self.gerar_ciclo(tempo_atual, indice=indice)
            ciclo['timestamp'] = tempo_atual
            dados.append(ciclo)
            tempo_atual += timedelta(seconds=passo_segundos)
            indice += 1

        df = pd.DataFrame(dados)
        logger.info(f"Simulação concluída! {len(df)} registros gerados.")

        nome_arquivo = arquivo_saida or f"dados_simulados_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        df.to_csv(nome_arquivo, index=False)
        logger.info(f"Dados salvos em: {nome_arquivo}")

        return df


def calcular_oee(df: pd.DataFrame, config: ConfiguracaoOEE) -> Dict:
    """Calcula os indicadores OEE a partir dos dados simulados ou reais."""
    df_producao = df[df['status_operacional'] == 'EM_PRODUCAO']

    if df_producao.empty:
        logger.warning("Sem dados de produção para calcular OEE")
        return {
            'disponibilidade': 0,
            'performance': 0,
            'qualidade': 0,
            'oee': 0,
            'classificacao': 'SEM DADOS',
        }

    total_pecas = df_producao['pecas_boas'].sum() + df_producao['pecas_defeituosas'].sum()
    total_boas = df_producao['pecas_boas'].sum()
    tempo_operacional = float(df_producao['tempo_ciclo_segundos'].sum())

    if 'timestamp' in df.columns and len(df) > 1:
        janela_segundos = (df['timestamp'].max() - df['timestamp'].min()).total_seconds()
        tempo_planejado = max(janela_segundos, 1.0)
    else:
        tempo_planejado = max(float(config.tempo_planejado_segundos), 1.0)

    disponibilidade = (tempo_operacional / tempo_planejado) * 100 if tempo_planejado > 0 else 0
    disponibilidade = min(100, max(0, disponibilidade))

    pecas_esperadas = tempo_operacional * config.capacidade_teorica_pecas_seg
    performance = (total_pecas / pecas_esperadas) * 100 if pecas_esperadas > 0 else 0
    performance = min(100, max(0, performance))

    qualidade = (total_boas / total_pecas * 100) if total_pecas > 0 else 0
    oee = (disponibilidade / 100) * (performance / 100) * (qualidade / 100) * 100

    if oee >= 85:
        classificacao = "EXCELENTE (World Class)"
    elif oee >= 70:
        classificacao = "MUITO BOM"
    elif oee >= 60:
        classificacao = "BOM"
    elif oee >= 50:
        classificacao = "REGULAR"
    else:
        classificacao = "CRÍTICO"

    return {
        'disponibilidade': round(disponibilidade, 2),
        'performance': round(performance, 2),
        'qualidade': round(qualidade, 2),
        'oee': round(oee, 2),
        'classificacao': classificacao,
        'total_pecas': int(total_pecas),
        'total_boas': int(total_boas),
        'total_defeitos': int(total_pecas - total_boas),
        'tempo_operacional': round(tempo_operacional, 2),
    }


def gerar_relatorio(indicadores: Dict, estatisticas: Dict) -> str:
    """Gera relatório formatado."""
    relatorio = f"""
{'='*70}
RELATÓRIO OEE - TECHMOB 4.0
{'='*70}

📊 INDICADORES OEE:
├── Disponibilidade: {indicadores.get('disponibilidade', 0):.1f}%
├── Performance: {indicadores.get('performance', 0):.1f}%
├── Qualidade: {indicadores.get('qualidade', 0):.1f}%
└── OEE GERAL: {indicadores.get('oee', 0):.1f}%
    Classificação: {indicadores.get('classificacao', 'N/A')}

📈 ESTATÍSTICAS DE PRODUÇÃO:
├── Total de Ciclos: {estatisticas.get('total_ciclos', 0)}
├── Tempo Total (h): {estatisticas.get('tempo_total_horas', 0):.2f}
├── Peças Produzidas: {indicadores.get('total_pecas', 0)}
│   ├── Boas: {indicadores.get('total_boas', 0)}
│   └── Defeituosas: {indicadores.get('total_defeitos', 0)}
└── Taxa de Defeito: {indicadores.get('total_defeitos', 0) / max(1, indicadores.get('total_pecas', 1)) * 100:.1f}%

{'='*70}
"""
    return relatorio


def main():
    """Função principal."""
    parser = argparse.ArgumentParser(description='Cálculo do OEE - TechMob 4.0')
    entrada = parser.add_mutually_exclusive_group(required=True)
    entrada.add_argument('--csv', help='Caminho do CSV de produção')
    entrada.add_argument('--simular', action='store_true', help='Executar simulação')
    parser.add_argument('--horas', type=float, default=8, help='Duração da simulação em horas')
    parser.add_argument('--seed', type=int, help='Semente opcional para reproduzir a simulação')
    parser.add_argument('--saida', help='Arquivo CSV para salvar os dados simulados')
    parser.add_argument('--relatorio', action='store_true', help='Gerar relatório detalhado')

    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)

    config = ConfiguracaoOEE()

    if args.simular:
        simulador = SimuladorBancadaSmart(config)
        df = simulador.gerar_lote(args.horas, arquivo_saida=args.saida)
    elif args.csv:
        if not os.path.exists(args.csv):
            logger.error('Arquivo não encontrado: %s', args.csv)
            sys.exit(1)
        df = pd.read_csv(args.csv)
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)
        logger.info(f'Carregados {len(df)} registros do arquivo')
    else:
        logger.error('Use --simular ou --csv para fornecer dados')
        sys.exit(1)

    indicadores = calcular_oee(df, config)
    estatisticas = {
        'total_ciclos': len(df),
        'tempo_total_horas': (df['timestamp'].max() - df['timestamp'].min()).total_seconds() / 3600,
    }

    logger.info('\n📊 RESULTADOS:')
    for key, value in indicadores.items():
        if key != 'classificacao':
            logger.info(f'{key}: {value}')
    logger.info(f"Classificação: {indicadores.get('classificacao', 'N/A')}")

    if args.relatorio:
        print(gerar_relatorio(indicadores, estatisticas))

    resultado_df = pd.DataFrame([indicadores])
    resultado_df.to_csv('resultado_oee.csv', index=False)
    logger.info('Resultados salvos em: resultado_oee.csv')


if __name__ == "__main__":
    main()

        'tempo_operacional': round(tempo_operacional, 2)
    }


def gerar_relatorio(indicadores: Dict, estatisticas: Dict) -> str:
<<<<<<< Updated upstream:data-science/simulador_oee.py
    """Gera relatório formatado"""
=======
    """Gera relatório formatado."""
>>>>>>> Stashed changes:data/simulador_oee.py
    relatorio = f"""
{'='*70}
RELATÓRIO OEE - TECHMOB 4.0
{'='*70}

📊 INDICADORES OEE:
├── Disponibilidade: {indicadores.get('disponibilidade', 0):.1f}%
├── Performance: {indicadores.get('performance', 0):.1f}%
├── Qualidade: {indicadores.get('qualidade', 0):.1f}%
└── OEE GERAL: {indicadores.get('oee', 0):.1f}%
    Classificação: {indicadores.get('classificacao', 'N/A')}

📈 ESTATÍSTICAS DE PRODUÇÃO:
├── Total de Ciclos: {estatisticas.get('total_ciclos', 0)}
├── Tempo Total (h): {estatisticas.get('tempo_total_horas', 0):.2f}
├── Peças Produzidas: {indicadores.get('total_pecas', 0)}
│   ├── Boas: {indicadores.get('total_boas', 0)}
│   └── Defeituosas: {indicadores.get('total_defeitos', 0)}
└── Taxa de Defeito: {indicadores.get('total_defeitos', 0) / max(1, indicadores.get('total_pecas', 1)) * 100:.1f}%

{'='*70}
"""
    return relatorio


def main():
<<<<<<< Updated upstream:data-science/simulador_oee.py
    """Função principal"""
    parser = argparse.ArgumentParser(description='Cálculo do OEE - TechMob 4.0')
    entrada = parser.add_mutually_exclusive_group(required=True)
    entrada.add_argument('--csv', help='Caminho do CSV de produção')
    entrada.add_argument('--simular', action='store_true', help='Executar simulação')
    parser.add_argument('--horas', type=float, default=8, help='Duração da simulação em horas')
    parser.add_argument('--seed', type=int, help='Semente opcional para reproduzir a simulação')
    parser.add_argument('--saida', help='Arquivo CSV para salvar os dados simulados')
    parser.add_argument('--relatorio', action='store_true', help='Gerar relatório detalhado')
    
    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)
    
    config = ConfiguracaoOEE()
    
    # Carrega ou gera dados
    if args.simular:
        simulador = SimuladorBancadaSmart(config)
        df = simulador.gerar_lote(args.horas, arquivo_saida=args.saida)
    elif args.csv:
        if not os.path.exists(args.csv):
            logger.error(f"Arquivo não encontrado: {args.csv}")
            sys.exit(1)
        df = pd.read_csv(args.csv)
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)
        logger.info(f"Carregados {len(df)} registros do arquivo")

    # Calcula indicadores
    indicadores = calcular_oee(df, config)
    
    # Estatísticas
    estatisticas = {
        'total_ciclos': len(df),
        'tempo_total_horas': (df['timestamp'].max() - df['timestamp'].min()).total_seconds() / 3600
    }
    
    # Exibe resultados
    logger.info("\n📊 RESULTADOS:")
    for key, value in indicadores.items():
        if key != 'classificacao':
            logger.info(f"{key}: {value}")
    logger.info(f"Classificação: {indicadores.get('classificacao', 'N/A')}")
    
    if args.relatorio:
        print(gerar_relatorio(indicadores, estatisticas))
    
    # Salva resultado
    resultado_df = pd.DataFrame([indicadores])
    resultado_df.to_csv('resultado_oee.csv', index=False)
    logger.info("Resultados salvos em: resultado_oee.csv")
=======
    """Função principal."""
    parser = argparse.ArgumentParser(description='Cálculo do OEE - TechMob 4.0')
    entrada = parser.add_mutually_exclusive_group(required=True)
    entrada.add_argument('--csv', help='Caminho do CSV de produção')
    entrada.add_argument('--simular', action='store_true', help='Executar simulação')
    parser.add_argument('--horas', type=float, default=8, help='Duração da simulação em horas')
    parser.add_argument('--seed', type=int, help='Semente opcional para reproduzir a simulação')
    parser.add_argument('--saida', help='Arquivo CSV para salvar os dados simulados')
    parser.add_argument('--relatorio', action='store_true', help='Gerar relatório detalhado')

    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)

    config = ConfiguracaoOEE()

    if args.simular:
        simulador = SimuladorBancadaSmart(config)
        df = simulador.gerar_lote(args.horas, arquivo_saida=args.saida)
    elif args.csv:
        if not os.path.exists(args.csv):
            logger.error('Arquivo não encontrado: %s', args.csv)
            sys.exit(1)
        df = pd.read_csv(args.csv)
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)
        logger.info(f'Carregados {len(df)} registros do arquivo')
    else:
        logger.error('Use --simular ou --csv para fornecer dados')
        sys.exit(1)

    indicadores = calcular_oee(df, config)
    estatisticas = {
        'total_ciclos': len(df),
        'tempo_total_horas': (df['timestamp'].max() - df['timestamp'].min()).total_seconds() / 3600,
    }

    logger.info('\n📊 RESULTADOS:')
    for key, value in indicadores.items():
        if key != 'classificacao':
            logger.info(f'{key}: {value}')
    logger.info(f"Classificação: {indicadores.get('classificacao', 'N/A')}")

    if args.relatorio:
        print(gerar_relatorio(indicadores, estatisticas))

    resultado_df = pd.DataFrame([indicadores])
    resultado_df.to_csv('resultado_oee.csv', index=False)
    logger.info('Resultados salvos em: resultado_oee.csv')
>>>>>>> Stashed changes:data/simulador_oee.py


if __name__ == "__main__":
    main()
"""
TechMob 4.0 - Processador OEE com Simulação
Versão adaptada para Windows
"""

import argparse
import logging
import os
import random
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from enum import Enum
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd

# Configuração de logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger("oee_calculator")


class StatusOperacional(Enum):
    """Status operacionais da máquina"""
    PRODUCAO = "EM_PRODUCAO"
    PARADA = "PARADA_PROGRAMADA"
    FALHA = "FALHA_NAO_PROGRAMADA"
    MANUTENCAO = "MANUTENCAO_PREVENTIVA"


@dataclass
class ConfiguracaoOEE:
    """Configuração dos parâmetros do OEE"""
    tempo_planejado_segundos: float = 8 * 3600  # 8 horas
    capacidade_teorica_pecas_seg: float = 1 / 8.5
    nome_maquina: str = "BANCADA_SMART_01"
    
    # Parâmetros de simulação realista
    tempo_ciclo_base: float = 8.5
    variabilidade_ciclo: float = 1.2
    probabilidade_falha: float = 0.02
    taxa_defeito_base: float = 0.02


class SimuladorBancadaSmart:
    """Simulador da Bancada Smart 4.0 para Windows"""
    
    def __init__(self, config: Optional[ConfiguracaoOEE] = None):
        self.config = config or ConfiguracaoOEE()
        self._estado_atual = StatusOperacional.PRODUCAO
        self._contador_pecas = 0
        self._tempo_restante_falha = 0
    
    def gerar_ciclo(self, timestamp: Optional[datetime] = None) -> Dict:
        """Gera um ciclo de produção"""
        if timestamp is None:
            timestamp = datetime.now(timezone.utc)
        
        # Determina status
        if self._estado_atual == StatusOperacional.FALHA:
            self._tempo_restante_falha -= 1
            if self._tempo_restante_falha <= 0:
                self._estado_atual = StatusOperacional.PRODUCAO
                tempo_ciclo = self.config.tempo_ciclo_base
                status = StatusOperacional.PRODUCAO
            else:
                tempo_ciclo = 1.0
                status = StatusOperacional.FALHA
        else:
            # Decisão de falha
            if random.random() < self.config.probabilidade_falha:
                self._estado_atual = StatusOperacional.FALHA
                self._tempo_restante_falha = random.randint(30, 180)  # 30s a 3min
                tempo_ciclo = 1.0
                status = StatusOperacional.FALHA
            else:
                # Produção normal
                tempo_ciclo = max(3.0, random.gauss(
                    self.config.tempo_ciclo_base,
                    self.config.variabilidade_ciclo
                ))
                status = StatusOperacional.PRODUCAO
                self._contador_pecas += 1
        
        # Calcula peças
        if status == StatusOperacional.PRODUCAO:
            # 1 peça por ciclo com possibilidade de múltiplas
            pecas_ciclo = 1
            taxa_defeito = self.config.taxa_defeito_base + random.gauss(0, 0.005)
            taxa_defeito = max(0, min(0.1, taxa_defeito))
            
            pecas_defeituosas = int(pecas_ciclo * taxa_defeito)
            pecas_boas = pecas_ciclo - pecas_defeituosas
        else:
            pecas_boas = 0
            pecas_defeituosas = 0
        
        return {
            'timestamp': timestamp,
            'bancada_id': self.config.nome_maquina,
            'status_operacional': status.value,
            'pecas_boas': pecas_boas,
            'pecas_defeituosas': pecas_defeituosas,
            'tempo_ciclo_segundos': round(tempo_ciclo, 2)
        }
    
    def gerar_lote(self, duracao_horas: float = 8) -> pd.DataFrame:
        """Gera um lote completo de dados"""
        dados = []
        tempo_inicio = datetime.now(timezone.utc) - timedelta(hours=duracao_horas)
        tempo_atual = tempo_inicio
        tempo_fim = tempo_inicio + timedelta(hours=duracao_horas)
        
        logger.info(f"Iniciando simulação de {duracao_horas} horas...")
        
        while tempo_atual < tempo_fim:
            ciclo = self.gerar_ciclo(tempo_atual)
            dados.append(ciclo)
            tempo_atual += timedelta(seconds=ciclo['tempo_ciclo_segundos'])
        
        df = pd.DataFrame(dados)
        logger.info(f"Simulação concluída! {len(df)} ciclos gerados.")
        
        # Salva automaticamente
        nome_arquivo = f"dados_simulados_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        df.to_csv(nome_arquivo, index=False)
        logger.info(f"Dados salvos em: {nome_arquivo}")
        
        return df


def calcular_oee(df: pd.DataFrame, config: ConfiguracaoOEE) -> Dict:
    """Calcula os indicadores OEE"""
    # Filtra apenas produção
    df_producao = df[df['status_operacional'] == 'EM_PRODUCAO']
    
    if df_producao.empty:
        logger.warning("Sem dados de produção para calcular OEE")
        return {
            'disponibilidade': 0,
            'performance': 0,
            'qualidade': 0,
            'oee': 0,
            'classificacao': 'SEM DADOS'
        }
    
    total_pecas = df_producao['pecas_boas'].sum() + df_producao['pecas_defeituosas'].sum()
    total_boas = df_producao['pecas_boas'].sum()
    tempo_operacional = df_producao['tempo_ciclo_segundos'].sum()
    
    # 1. Disponibilidade
    if config.tempo_planejado_segundos > 0:
        disponibilidade = (tempo_operacional / config.tempo_planejado_segundos) * 100
    else:
        disponibilidade = 0
    disponibilidade = min(100, disponibilidade)
    
    # 2. Performance
    if tempo_operacional > 0 and config.capacidade_teorica_pecas_seg > 0:
        pecas_esperadas = tempo_operacional * config.capacidade_teorica_pecas_seg
        performance = (total_pecas / pecas_esperadas) * 100 if pecas_esperadas > 0 else 0
    else:
        performance = 0
    performance = min(100, performance)
    
    # 3. Qualidade
    qualidade = (total_boas / total_pecas * 100) if total_pecas > 0 else 0
    
    # OEE
    oee = (disponibilidade / 100) * (performance / 100) * (qualidade / 100) * 100
    
    # Classificação
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
        'tempo_operacional': round(tempo_operacional, 2)
    }


def gerar_relatorio(indicadores: Dict, estatisticas: Dict) -> str:
    """Gera relatório formatado"""
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
    """Função principal"""
    parser = argparse.ArgumentParser(description='Cálculo do OEE - TechMob 4.0')
    parser.add_argument('--csv', help='Caminho do CSV de produção')
    parser.add_argument('--simular', action='store_true', help='Executar simulação')
    parser.add_argument('--horas', type=float, default=8, help='Duração da simulação em horas')
    parser.add_argument('--relatorio', action='store_true', help='Gerar relatório detalhado')
    
    args = parser.parse_args()
    
    config = ConfiguracaoOEE()
    
    # Carrega ou gera dados
    if args.simular:
        simulador = SimuladorBancadaSmart(config)
        df = simulador.gerar_lote(args.horas)
    elif args.csv:
        if not os.path.exists(args.csv):
            logger.error(f"Arquivo não encontrado: {args.csv}")
            sys.exit(1)
        df = pd.read_csv(args.csv)
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)
        logger.info(f"Carregados {len(df)} registros do arquivo")
    else:
        logger.error("Use --simular ou --csv para fornecer dados")
        sys.exit(1)
    
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


if __name__ == "__main__":
    main()
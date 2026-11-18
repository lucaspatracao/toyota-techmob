"""
TechMob 4.0 - Cronometragem a partir das gravações reais da bancada.

Lê uma ou mais gravações .jsonl do coletor_planta.py, identifica os pedidos
com as mesmas regras do oee_calculator.py (e do backend) e, com os pedidos
concluídos:
  1. calcula o tempo padrão de cada etapa: a MEDIANA do tempo operando
     (duração da etapa menos o tempo em parada), que não se deixa levar por
     um pedido fora da curva;
  2. aponta o gargalo: a etapa com maior variação (coeficiente de variação)
     e a etapa com maior atraso da mediana sobre o padrão atual.

Com --atualizar, grava as medianas no ciclo_padrao.json (o simulador e o
oee_calculator.py passam a usar os tempos reais). Lembre de levar o mesmo
valor para o backend (TEMPO_PADRAO_*_S) e para o seed do schema.sql.

Uso:
    python analisar_cronometragem.py                       # todas as gravações de gravacoes/
    python analisar_cronometragem.py gravacoes/sessao_A.jsonl gravacoes/sessao_B.jsonl
    python analisar_cronometragem.py gravacoes/ --atualizar
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import pandas as pd

import oee_calculator as oee

PASTA_GRAVACOES = oee.PASTA / "gravacoes"


def listar_gravacoes(caminhos: list[Path]) -> list[Path]:
    """Aceita arquivos e pastas (o PowerShell não expande *.jsonl)."""
    arquivos: list[Path] = []
    for caminho in caminhos:
        if caminho.is_dir():
            arquivos.extend(sorted(caminho.glob("*.jsonl")))
        elif caminho.exists():
            arquivos.append(caminho)
        else:
            raise FileNotFoundError(f"Gravação não encontrada: {caminho}")
    return arquivos


def tabela_etapas(arquivos: list[Path], cfg: dict) -> pd.DataFrame:
    """Uma linha por etapa de pedido concluído, de todas as gravações."""
    linhas = []
    for arquivo in arquivos:
        for pedido in oee.detectar_pedidos(oee.ler_gravacao(arquivo), cfg):
            if pedido.situacao != oee.CONCLUIDO:
                continue
            for etapa in pedido.etapas.values():
                linhas.append({
                    "gravacao": arquivo.name,
                    "pedido": pedido.numero,
                    "estacao": etapa.estacao,
                    "duracao_s": etapa.duracao_s,
                    "tempo_parado_s": etapa.tempo_parado_s,
                    "operando_s": etapa.duracao_s - etapa.tempo_parado_s,
                })
    return pd.DataFrame(linhas, columns=["gravacao", "pedido", "estacao", "duracao_s",
                                         "tempo_parado_s", "operando_s"])


def resumo_por_estacao(etapas: pd.DataFrame, cfg: dict) -> pd.DataFrame:
    """Estatísticas do tempo operando de cada estação, comparadas ao padrão atual."""
    padrao_atual = {e["estacao"]: float(e["duracao_s"]) for e in cfg["etapas"]}
    grupos = etapas.groupby("estacao")["operando_s"]
    resumo = pd.DataFrame({
        "pedidos": grupos.count(),
        "mediana_s": grupos.median(),
        "media_s": grupos.mean(),
        "desvio_s": grupos.std(ddof=0),
        "min_s": grupos.min(),
        "max_s": grupos.max(),
    })
    resumo["cv_pct"] = resumo["desvio_s"] / resumo["media_s"] * 100
    resumo["padrao_atual_s"] = [padrao_atual.get(estacao) for estacao in resumo.index]
    resumo["atraso_s"] = resumo["mediana_s"] - resumo["padrao_atual_s"]
    # Mantém a ordem da bancada (estoque, manipulação, separação).
    ordem = [e for e in oee.ESTACOES if e in resumo.index]
    return resumo.loc[ordem]


def gargalos(resumo: pd.DataFrame) -> dict[str, str]:
    """Estação com maior variação e estação com maior atraso sobre o padrão."""
    return {
        "maior_variacao": resumo["cv_pct"].idxmax(),
        "maior_atraso": resumo["atraso_s"].idxmax(),
    }


def atualizar_ciclo(caminho: Path, resumo: pd.DataFrame) -> dict:
    """Grava as medianas (0,1 s) no ciclo_padrao.json e devolve a config nova."""
    cfg = json.loads(caminho.read_text(encoding="utf-8"))
    for etapa in cfg["etapas"]:
        if etapa["estacao"] in resumo.index:
            etapa["duracao_s"] = round(float(resumo.loc[etapa["estacao"], "mediana_s"]), 1)
    caminho.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return cfg


def main(argv: list[str] | None = None) -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # acentos no console do Windows

    parser = argparse.ArgumentParser(description="Tempo padrão e gargalo a partir das gravações da bancada.")
    parser.add_argument("gravacoes", type=Path, nargs="*", default=[PASTA_GRAVACOES],
                        help="arquivos .jsonl ou pastas (padrão: gravacoes/)")
    parser.add_argument("--config", type=Path, default=oee.CONFIG_PADRAO, help="ciclo_padrao.json")
    parser.add_argument("--atualizar", action="store_true", help="grava as medianas no ciclo_padrao.json")
    args = parser.parse_args(argv)

    cfg = oee.carregar_config(args.config)
    arquivos = listar_gravacoes(args.gravacoes)
    etapas = tabela_etapas(arquivos, cfg)
    if etapas.empty:
        print("Nenhum pedido concluído nas gravações.")
        return

    resumo = resumo_por_estacao(etapas, cfg)
    pedidos = etapas.groupby(["gravacao", "pedido"]).ngroups
    print(f"Gravações: {len(arquivos)}  |  pedidos concluídos: {pedidos}")
    print("Tempo operando por estação (duração da etapa menos paradas), em segundos:\n")
    print(resumo.to_string(float_format=lambda v: f"{v:.1f}"))

    total = resumo["mediana_s"].sum()
    print(f"\nTempo padrão sugerido (mediana): {', '.join(f'{e} {v:.1f} s' for e, v in resumo['mediana_s'].items())}"
          f"  |  pedido {total:.1f} s")
    destaque = gargalos(resumo)
    print(f"Gargalo por variação: {destaque['maior_variacao']} "
          f"(CV {resumo.loc[destaque['maior_variacao'], 'cv_pct']:.1f}%)")
    print(f"Gargalo por atraso sobre o padrão: {destaque['maior_atraso']} "
          f"({resumo.loc[destaque['maior_atraso'], 'atraso_s']:+.1f} s)")

    if args.atualizar:
        atualizar_ciclo(args.config, resumo)
        print(f"\n{args.config} atualizado com as medianas. Use o mesmo total ({total:.1f} s) no seed "
              f"do schema.sql e os tempos por estação nas variáveis TEMPO_PADRAO_*_S do backend.")


if __name__ == "__main__":
    main()

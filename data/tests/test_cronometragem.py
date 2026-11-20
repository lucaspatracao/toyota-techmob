"""Testes do analisar_cronometragem.py (tempo padrão e gargalo)."""
import json
import shutil
from pathlib import Path

import oee_calculator as oee
import analisar_cronometragem as crono

GRAVACOES = Path(__file__).parent / "gravacoes"


def test_mediana_do_tempo_operando_por_estacao():
    cfg = oee.carregar_config()
    etapas = crono.tabela_etapas([GRAVACOES / "sintetica_seed12.jsonl"], cfg)

    resumo = crono.resumo_por_estacao(etapas, cfg)

    # 4 pedidos concluídos; o interrompido fica fora.
    assert resumo["pedidos"].tolist() == [4, 4, 4]
    assert resumo.index.tolist() == ["estoque", "manipulacao", "separacao"]
    # Tempo operando = duração - parada (ex.: separação do pedido 3: 65 - 37 = 28).
    # Separação: 24, 23, 28, 26 -> mediana 25,0 (padrão 25). Estoque: 20, 21, 19, 21
    # -> mediana 20,5, meio segundo acima do padrão de 20 s.
    assert resumo["mediana_s"].tolist() == [20.5, 33.5, 25.0]
    assert crono.gargalos(resumo) == {"maior_variacao": "separacao", "maior_atraso": "estoque"}


def test_atualizar_grava_as_medianas_no_ciclo_padrao(tmp_path):
    ciclo = tmp_path / "ciclo_padrao.json"
    shutil.copy(oee.CONFIG_PADRAO, ciclo)

    crono.main([str(GRAVACOES), "--config", str(ciclo), "--atualizar"])

    etapas = {e["estacao"]: e["duracao_s"] for e in json.loads(ciclo.read_text(encoding="utf-8"))["etapas"]}
    assert etapas == {"estoque": 21.0, "manipulacao": 34.0, "separacao": 26.0}

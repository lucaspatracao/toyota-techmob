"""
Testes do OEE por pedido (oee_calculator.py).

As gravações em tests/gravacoes/ estão no formato do coletor_planta.py e são
as mesmas de backend/src/test/resources/gravacoes/: Python e Java têm que
chegar nos mesmos números.

  exemplo_80.jsonl         o exemplo do plano: pedido de 100 s com 12 s
                           parado e padrão de 80 s -> OEE 80,0%.
  sintetica_seed12.jsonl   simulador_planta.py no modo sintético (--seed 12),
                           amostrado a cada 1 s como o coletor faria, com uma
                           queda de conexão de 20 s no meio do 4º pedido.
"""
from pathlib import Path

import pandas as pd
import pytest

import oee_calculator as oee

GRAVACOES = Path(__file__).parent / "gravacoes"


@pytest.fixture(scope="module")
def cfg():
    return oee.carregar_config()


def pedidos_da(nome, cfg):
    return oee.detectar_pedidos(oee.ler_gravacao(GRAVACOES / nome), cfg)


def pct(resultado):
    return tuple(round(v * 100, 3) for v in
                 (resultado.disponibilidade, resultado.performance, resultado.qualidade, resultado.oee))


# ---- cálculo -----------------------------------------------------------

def test_exemplo_do_plano_da_80_por_cento():
    resultado = oee.calcular_oee(lead_time_s=100, tempo_parado_s=12, tempo_padrao_s=80, qualidade=1)

    assert pct(resultado) == (88.0, 90.909, 100.0, 80.0)


def test_pedido_reprovado_zera_o_oee():
    resultado = oee.calcular_oee(100, 12, 80, qualidade=0)

    assert resultado.qualidade == 0
    assert resultado.oee == 0


def test_performance_limitada_a_100_por_cento():
    # Mais rápido que o padrão: 70 s operando para um padrão de 80 s.
    resultado = oee.calcular_oee(70, 0, 80, 1)

    assert resultado.performance == 1.0
    assert resultado.oee == 1.0


def test_pedido_todo_parado_nao_divide_por_zero():
    resultado = oee.calcular_oee(30, 30, 80, 1)

    assert (resultado.disponibilidade, resultado.performance, resultado.oee) == (0, 0, 0)


def test_acumulado_soma_os_tempos_em_vez_de_tirar_media():
    curto = oee.Pedido(1, 0, "", fim_s=60, situacao=oee.CONCLUIDO, tempo_parado_s=0)
    longo = oee.Pedido(2, 100, "", fim_s=300, situacao=oee.CONCLUIDO, tempo_parado_s=100)
    for pedido in (curto, longo):
        pedido.etapas["estoque"] = oee.Etapa("estoque", pedido.inicio_s, 80)

    total = oee.oee_acumulado([curto, longo])

    # Média dos OEEs daria (100% + 40%) / 2 = 70%; somando os tempos:
    # D = (260 - 100) / 260, P = mín(1; 160 / 160) = 1, OEE = 61,5%.
    assert round(total.oee * 100, 1) == 61.5
    assert round((curto.oee.oee + longo.oee.oee) / 2 * 100, 1) == 70.0


# ---- detecção na gravação ----------------------------------------------

def test_gravacao_do_exemplo_detecta_o_pedido_de_80_por_cento(cfg):
    pedidos = pedidos_da("exemplo_80.jsonl", cfg)

    assert len(pedidos) == 1
    pedido = pedidos[0]
    assert pedido.situacao == oee.CONCLUIDO
    assert (pedido.lead_time_s, pedido.tempo_parado_s, pedido.tempo_padrao_s) == (100, 12, 80)
    assert pct(pedido.oee) == (88.0, 90.909, 100.0, 80.0)
    assert {e: (round(x.duracao_s, 3), round(x.tempo_parado_s, 3)) for e, x in pedido.etapas.items()} == {
        "estoque": (21, 0), "manipulacao": (47, 12), "separacao": (26, 0)}


def test_gravacao_sintetica_detecta_os_pedidos(cfg):
    pedidos = pedidos_da("sintetica_seed12.jsonl", cfg)

    resumo = [(p.numero, p.situacao, p.inicio_s, p.fim_s, round(p.tempo_parado_s, 3), p.reprovado)
              for p in pedidos]
    assert resumo == [
        (1, "CONCLUIDO", 5, 107, 21, False),
        (2, "CONCLUIDO", 137, 213, 0, True),
        (3, "CONCLUIDO", 245, 362, 37, False),
        (4, "INTERROMPIDO", 389, 418, 0, False),
        (5, "CONCLUIDO", 495, 610, 34, False),
    ]
    assert [None if p.oee is None else pct(p.oee)[3] for p in pedidos] == [78.431, 0.0, 68.376, None, 69.565]


def test_pedido_reprovado_pelo_contador(cfg):
    reprovado = pedidos_da("sintetica_seed12.jsonl", cfg)[1]

    assert reprovado.reprovado
    assert pct(reprovado.oee) == (100.0, 100.0, 0.0, 0.0)


def test_queda_de_conexao_interrompe_o_pedido_na_ultima_leitura_boa(cfg):
    interrompido = pedidos_da("sintetica_seed12.jsonl", cfg)[3]

    # Queda às 10:06:59 (t = 419 s): a última leitura boa foi 1 s antes.
    assert interrompido.situacao == oee.INTERROMPIDO
    assert interrompido.lead_time_s == 29
    assert interrompido.fim_ts == "2026-11-17T10:06:58.000"
    assert {e: x.duracao_s for e, x in interrompido.etapas.items()} == {"estoque": 22, "manipulacao": 7}
    assert interrompido.oee is None  # fora do OEE


def test_acumulado_da_gravacao_ignora_o_interrompido(cfg):
    total = oee.oee_acumulado(pedidos_da("sintetica_seed12.jsonl", cfg))

    assert pct(total) == (77.561, 100.0, 75.0, 58.171)


def leitura(aguardando, reprovados, estoque="AGUARDANDO", produzindo=None):
    return {"operacao_estoque": estoque, "operacao_manipulacao": "AGUARDANDO",
            "operacao_separacao": "AGUARDANDO", "operacao_string": estoque,
            "aguardando": aguardando, "produzindo": 1 - aguardando if produzindo is None else produzindo,
            "total_pedidos": 0, "pedidos_reprovados": reprovados, "pedidos15min": 0,
            "Temperatura": 0, "Umidade": 0}


def test_contador_que_diminui_e_reinicio_e_nao_reprovacao():
    detector = oee.DetectorPedidos({"estoque": 80})
    detector.leitura(leitura(1, 5), 0)
    detector.leitura(leitura(0, 5, "PRODUZINDO"), 1)
    detector.leitura(leitura(0, 0, "PRODUZINDO"), 30)  # bancada reiniciou o contador
    detector.leitura(leitura(1, 0), 60)

    assert detector.fechados[0].reprovado is False


def test_queda_curta_nao_interrompe_o_pedido():
    detector = oee.DetectorPedidos({"estoque": 80})
    detector.leitura(leitura(1, 0), 0)
    detector.leitura(leitura(0, 0, "PRODUZINDO"), 1)
    detector.erro(20)
    detector.leitura(leitura(0, 0, "PRODUZINDO"), 28)  # voltou em 8 s
    detector.leitura(leitura(1, 0), 81)

    pedido = detector.fechados[0]
    assert pedido.situacao == oee.CONCLUIDO
    assert pedido.lead_time_s == 80


def test_pedido_so_comeca_com_a_transicao_de_aguardando():
    detector = oee.DetectorPedidos({"estoque": 80})
    # O coletor começou com a bancada já produzindo: não sabemos o início.
    detector.leitura(leitura(0, 0, "PRODUZINDO"), 0)
    detector.leitura(leitura(1, 0), 50)

    assert detector.fechados == []


# ---- linha de comando --------------------------------------------------

def test_cli_mostra_a_tabela_e_grava_o_csv(tmp_path, capsys):
    oee.main([str(GRAVACOES / "sintetica_seed12.jsonl"), "--saida", str(tmp_path)])

    saida = capsys.readouterr().out
    assert "Pedidos concluídos: 4 (reprovados: 1), interrompidos: 1" in saida
    assert "OEE acumulado (soma dos tempos): 58.2%" in saida
    csv = pd.read_csv(tmp_path / "pedidos_sintetica_seed12.csv")
    assert len(csv) == 5
    assert csv["oee"].round(3).tolist()[:3] == [78.431, 0.0, 68.376]

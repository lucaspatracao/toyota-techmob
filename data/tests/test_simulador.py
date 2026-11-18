"""O simulador avisa a origem do dado no cabeçalho X-TechMob-Fonte."""
import json
import threading
import urllib.request
from http.server import ThreadingHTTPServer

import pytest

import simulador_planta as sim


@pytest.fixture
def servidor():
    criados = []

    def subir(modo):
        fonte = sim.SimuladorSintetico(sim.carregar_config(sim.CONFIG_PADRAO), seed=1)
        handler = sim.criar_handler(fonte, lambda: 0.0, verboso=False, nome_fonte=modo)
        httpd = ThreadingHTTPServer(("127.0.0.1", 0), handler)
        threading.Thread(target=httpd.serve_forever, daemon=True).start()
        criados.append(httpd)
        return f"http://127.0.0.1:{httpd.server_address[1]}{sim.ROTA}"

    yield subir
    for httpd in criados:
        httpd.shutdown()
        httpd.server_close()


@pytest.mark.parametrize("modo", ["sintetico", "replay"])
def test_resposta_traz_cabecalho_da_fonte(servidor, modo):
    with urllib.request.urlopen(servidor(modo), timeout=5) as resposta:
        corpo = json.loads(resposta.read())

        assert resposta.headers["X-TechMob-Fonte"] == modo
    assert list(corpo)[:4] == ["operacao_estoque", "operacao_manipulacao",
                               "operacao_separacao", "operacao_string"]
    assert corpo["aguardando"] == 1

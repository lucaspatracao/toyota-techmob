import io
import json

import pytest

import coletor_planta as coletor


@pytest.mark.parametrize("resposta_invalida", [b"[]", b"null"])
def test_coleta_continua_depois_de_resposta_que_nao_e_objeto(
    monkeypatch, tmp_path, resposta_invalida
):
    respostas = iter((b'{"aguardando": 1}', resposta_invalida, b'{"aguardando": 0}'))

    def abrir(url, timeout):
        corpo = next(respostas, None)
        if corpo is None:
            raise KeyboardInterrupt
        return io.BytesIO(corpo)

    monkeypatch.setattr(coletor._OPENER, "open", abrir)
    caminho = tmp_path / "coleta.jsonl"
    gravador = coletor.Gravador(caminho)
    try:
        with pytest.raises(KeyboardInterrupt):
            coletor.coletar("http://127.0.0.1/dadosPlanta", 0, 1, gravador, coletor.Resumo())
    finally:
        gravador.fechar()

    registros = [json.loads(linha) for linha in caminho.read_text(encoding="utf-8").splitlines()]
    assert [registro["tipo"] for registro in registros] == ["leitura", "erro", "leitura"]
    assert registros[1]["erro"] == "json_invalido: esperado um objeto JSON"
    assert registros[1]["bruto"] == resposta_invalida.decode("utf-8")
    assert registros[2]["dados"] == {"aguardando": 0}

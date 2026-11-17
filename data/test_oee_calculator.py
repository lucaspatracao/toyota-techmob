import logging
import subprocess
import sys
from pathlib import Path
from unittest.mock import Mock

import pandas as pd
import pytest

import oee_calculator as oee


@pytest.fixture
def csv_producao(tmp_path):
    caminho = tmp_path / "producao.csv"
    caminho.write_text(
        ",".join(oee.COLUNAS_ESPERADAS) + "\n"
        "2026-08-17T09:00:00.123Z,SMART_01,EM_PRODUCAO,1,1,20\n"
        "2026-08-17T08:00:00Z,SMART_01,EM_PRODUCAO,1,0,10\n"
        "2026-08-17T08:30:00Z,SMART_01,PARADA,0,0,50\n"
        "2026-08-17T08:10:00Z,SMART_02,EM_PRODUCAO,1,0,11\n",
        encoding="utf-8",
    )
    return caminho


@pytest.mark.parametrize("com_cabecalho", [True, False])
def test_le_csv_ordenado_com_ou_sem_cabecalho(csv_producao, com_cabecalho):
    if not com_cabecalho:
        linhas = csv_producao.read_text(encoding="utf-8").splitlines(keepends=True)
        csv_producao.write_text("".join(linhas[1:]), encoding="utf-8")

    registros = oee.ler_csv_producao(csv_producao)

    assert registros.columns.tolist() == oee.COLUNAS_ESPERADAS
    assert registros["tempo_ciclo_segundos"].tolist() == [10, 11, 50, 20]
    assert registros["timestamp"].iloc[-1] == pd.Timestamp("2026-08-17T09:00:00.123Z")


def test_rejeita_csv_com_colunas_ausentes(tmp_path):
    caminho = tmp_path / "incompleto.csv"
    caminho.write_text("timestamp,bancada_id\n", encoding="utf-8")

    with pytest.raises(ValueError, match="Colunas ausentes.*tempo_ciclo_segundos"):
        oee.ler_csv_producao(caminho)


@pytest.mark.parametrize("conteudo", ["", ",".join(oee.COLUNAS_ESPERADAS) + "\n"])
def test_processar_csv_vazio_sem_gravar(tmp_path, monkeypatch, conteudo):
    caminho = tmp_path / "vazio.csv"
    caminho.write_text(conteudo, encoding="utf-8")
    gravar = Mock()
    monkeypatch.setattr(oee, "gravar_indicador_mysql", gravar)

    assert oee.processar_csv(caminho) is None
    gravar.assert_not_called()


def test_calcula_oee_com_parada_e_refugo(csv_producao):
    registros = oee.filtrar_por_maquina(oee.ler_csv_producao(csv_producao), "SMART_01")

    resultado = oee.calcular_oee(registros, oee.ConfiguracaoOEE(100, 0.2))

    assert resultado == {
        "disponibilidade": 30.0,
        "performance": 50.0,
        "qualidade": 66.67,
        "oee": 10.0,
        "tempo_operacional_real_segundos": 30.0,
        "quantidade_total_produzida": 3,
        "pecas_boas": 2,
        "pecas_defeituosas": 1,
    }


def test_calcula_oee_sem_producao_e_rejeita_tempo_planejado_zero(csv_producao):
    registros = oee.ler_csv_producao(csv_producao)
    paradas = registros.loc[registros["status_operacional"] == "PARADA"]

    resultado = oee.calcular_oee(paradas, oee.ConfiguracaoOEE())

    assert all(valor == 0 for valor in resultado.values())
    with pytest.raises(ValueError, match="tempo_planejado_segundos"):
        oee.calcular_oee(paradas, oee.ConfiguracaoOEE(tempo_planejado_segundos=0))


def test_media_movel_por_periodo(csv_producao):
    registros = oee.filtrar_por_maquina(oee.ler_csv_producao(csv_producao), "SMART_01")
    config = oee.ConfiguracaoOEE(100, 0.2)

    tendencia = oee.media_movel_oee(registros, config, janela="30min")

    assert tendencia["oee"].tolist() == [5.0, 0.0, 5.0]
    assert tendencia["oee_media_movel"].tolist() == [5.0, 2.5, 3.33]


def test_analisar_cronometragem_ignora_paradas_e_tempos_invalidos():
    registros = pd.DataFrame(
        {
            "status_operacional": ["EM_PRODUCAO"] * 8 + ["PARADA"],
            "tempo_ciclo_segundos": [8, "10", 12, None, 0, -1, float("inf"), "erro", 50],
        }
    )

    resultado = oee.analisar_cronometragem(registros, oee.ConfiguracaoOEE())

    assert resultado == pytest.approx(
        {
            "registros_em_producao": 8,
            "ciclos_validos": 3,
            "ciclos_sem_tempo_valido": 5,
            "tempo_ciclo_ideal": 10.0,
            "ciclos_acima_do_ideal": 1,
            "tempo_ciclo_medio": 10.0,
            "tempo_ciclo_desvio_padrao": (8 / 3) ** 0.5,
            "tempo_ciclo_min": 8.0,
            "tempo_ciclo_max": 12.0,
            "tempo_ciclo_mediano": 10.0,
            "tempo_ciclo_p90": 11.6,
        }
    )


def test_processar_csv_com_tempos_invalidos(tmp_path, caplog):
    caminho = tmp_path / "tempos_invalidos.csv"
    caminho.write_text(
        "2026-08-17T08:00:00Z,SMART_01,EM_PRODUCAO,1,0,\n"
        "2026-08-17T08:01:00Z,SMART_01,EM_PRODUCAO,1,0,erro\n"
        "2026-08-17T08:02:00Z,SMART_01,EM_PRODUCAO,1,0,inf\n"
        "2026-08-17T08:03:00Z,SMART_01,EM_PRODUCAO,1,0,-10\n",
        encoding="utf-8",
    )

    with caplog.at_level(logging.INFO):
        indicadores = oee.processar_csv(caminho, gravar_banco=False, cronometragem=True)

    assert indicadores["tempo_operacional_real_segundos"] == 0
    assert "'ciclos_validos': 0" in caplog.text
    assert "'ciclos_sem_tempo_valido': 4" in caplog.text
    assert "'tempo_ciclo_p90'" not in caplog.text


def test_processar_csv_envia_intervalo_e_indicadores_ao_banco(csv_producao, monkeypatch):
    gravar = Mock()
    monkeypatch.setattr(oee, "gravar_indicador_mysql", gravar)

    resultado = oee.processar_csv(csv_producao, maquina_id=7, bancada_id="SMART_02")

    instante = pd.Timestamp("2026-08-17T08:10:00Z").to_pydatetime()
    gravar.assert_called_once_with(
        maquina_id=7,
        indicadores=resultado,
        periodo_inicio=instante,
        periodo_fim=instante,
    )
    assert resultado["quantidade_total_produzida"] == 1


def test_cli_analisa_cronometragem_e_tendencia(csv_producao):
    execucao = subprocess.run(
        [
            sys.executable,
            str(Path(oee.__file__).resolve()),
            "--csv",
            str(csv_producao),
            "--bancada-id",
            "SMART_01",
            "--cronometragem",
            "--tendencia",
        ],
        capture_output=True,
        text=True,
        check=True,
    )

    assert "'ciclos_validos': 2" in execucao.stderr
    assert "'tempo_ciclo_medio': 15.0" in execucao.stderr
    assert "oee_media_movel" in execucao.stderr


def test_varredura_tenta_novamente_apos_erro_e_ignora_arquivo_inalterado(csv_producao, monkeypatch):
    processar = Mock(side_effect=[ValueError("falha de leitura"), {}])
    dormir = Mock(side_effect=[None, None, KeyboardInterrupt()])
    monkeypatch.setattr(oee, "processar_csv", processar)
    monkeypatch.setattr(oee.time, "sleep", dormir)

    with pytest.raises(KeyboardInterrupt):
        oee.varrer_diretorio(csv_producao.parent, gravar_banco=False, cronometragem=True)

    assert processar.call_count == 2
    assert processar.call_args.kwargs["caminho_csv"] == csv_producao
    assert processar.call_args.kwargs["cronometragem"] is True

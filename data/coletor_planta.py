"""
Coletor da Bancada Smart 4.0: grava o GET /dadosPlanta em arquivo.

Lê a API do Node-RED da bancada SEM alterar nada nela (somente leitura),
a cada N segundos, e grava em JSON Lines (um JSON por linha) apenas quando
o conteúdo muda. Cada linha guarda o horário da mudança, então a gravação
serve para:
  1) medir o tempo real de cada etapa do pedido (tempo padrão);
  2) descobrir todos os valores possíveis dos status (resumo ao encerrar);
  3) ser reproduzida depois pelo simulador_planta.py (modo replay).

Usa só a biblioteca padrão do Python: nada para instalar.

Uso:
    python coletor_planta.py
    python coletor_planta.py --url http://192.168.160.X:1880/dadosPlanta
    python coletor_planta.py --intervalo 0.5
Ctrl+C encerra a coleta e mostra o resumo.
"""
from __future__ import annotations

import argparse
import http.client
import json
import sys
import time
import urllib.request
from collections import defaultdict
from datetime import datetime
from pathlib import Path

URL_PADRAO = "http://localhost:1880/dadosPlanta"
PASTA_GRAVACOES = Path(__file__).resolve().parent / "gravacoes"
MAX_VALORES_NO_RESUMO = 15

# Sem proxy: a rede do SENAI pode ter proxy configurado no Windows, e o
# urllib o usaria até para IPs locais, derrubando a conexão com a bancada.
_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def hora() -> str:
    """Hora atual no formato HH:MM:SS, para o log do console."""
    return datetime.now().strftime("%H:%M:%S")


def ler_planta(url: str, timeout: float) -> tuple[dict | None, str | None, str | None]:
    """Faz uma leitura da API. Retorna (dados, texto_bruto, erro)."""
    try:
        with _OPENER.open(url, timeout=timeout) as resposta:
            bruto = resposta.read().decode("utf-8", errors="replace")
    except (OSError, http.client.HTTPException) as exc:
        return None, None, f"sem_conexao: {exc}"

    try:
        return json.loads(bruto), bruto, None
    except json.JSONDecodeError:
        # O Node-RED da bancada monta o JSON concatenando texto. Se alguma
        # variável global estiver indefinida, o resultado deixa de ser JSON
        # válido. Guardamos o texto bruto para análise.
        return None, bruto, "json_invalido"


class Gravador:
    """Escreve os registros em JSON Lines, um por linha, com horário."""

    def __init__(self, caminho: Path) -> None:
        caminho.parent.mkdir(parents=True, exist_ok=True)
        self.caminho = caminho
        self._arquivo = caminho.open("a", encoding="utf-8")
        self._t0 = time.monotonic()

    def registrar(self, tipo: str, **campos) -> None:
        linha = {
            "tipo": tipo,
            "ts": datetime.now().isoformat(timespec="milliseconds"),
            # Segundos desde o início da coleta (relógio monotônico: não
            # sofre com ajuste de horário do Windows). O replay usa este campo.
            "t_s": round(time.monotonic() - self._t0, 3),
            **campos,
        }
        self._arquivo.write(json.dumps(linha, ensure_ascii=False) + "\n")
        self._arquivo.flush()  # grava na hora: se o notebook desligar, nada se perde

    def fechar(self) -> None:
        self._arquivo.close()


class Resumo:
    """Acumula estatísticas da coleta e os valores distintos de cada campo."""

    def __init__(self) -> None:
        self.leituras = 0
        self.mudancas = 0
        self.erros: dict[str, int] = defaultdict(int)
        self.valores: dict[str, set[str]] = defaultdict(set)

    def observar(self, dados: dict) -> None:
        for campo, valor in dados.items():
            self.valores[campo].add(json.dumps(valor, ensure_ascii=False))

    def imprimir(self, caminho: Path) -> None:
        print("\n" + "=" * 60)
        print("RESUMO DA COLETA")
        print("=" * 60)
        print(f"Arquivo:   {caminho}")
        print(f"Leituras:  {self.leituras}")
        print(f"Mudanças:  {self.mudancas} (linhas gravadas)")
        for tipo, total in self.erros.items():
            print(f"Erros:     {tipo} = {total}")
        print("\nValores que apareceram em cada campo:")
        for campo, valores in self.valores.items():
            ordenados = sorted(valores)
            extra = len(ordenados) - MAX_VALORES_NO_RESUMO
            texto = ", ".join(ordenados[:MAX_VALORES_NO_RESUMO])
            if extra > 0:
                texto += f" ... (+{extra})"
            print(f"  {campo:<22} {texto}")


def imprimir_mudancas(anterior: dict | None, atual: dict) -> None:
    """Mostra no console só os campos que mudaram."""
    if anterior is None:
        print(f"[{hora()}] estado inicial: {json.dumps(atual, ensure_ascii=False)}")
        return
    for campo, valor in atual.items():
        if anterior.get(campo) != valor:
            print(f"[{hora()}] {campo}: {anterior.get(campo)} -> {valor}")


def coletar(url: str, intervalo: float, timeout: float,
            gravador: Gravador, resumo: Resumo) -> None:
    """Laço principal: lê, compara com a leitura anterior e grava se mudou."""
    anterior: dict | None = None
    tipo_erro_anterior: str | None = None
    proxima = time.monotonic()

    while True:
        dados, bruto, erro = ler_planta(url, timeout)
        resumo.leituras += 1

        if erro:
            tipo_erro = erro.split(":")[0]
            resumo.erros[tipo_erro] += 1
            # Registra só quando o tipo de erro muda, para não lotar o arquivo
            if tipo_erro != tipo_erro_anterior:
                gravador.registrar("erro", erro=erro, bruto=bruto)
                print(f"[{hora()}] [AVISO] {erro}")
            tipo_erro_anterior = tipo_erro
            anterior = None  # força gravar a primeira leitura boa após a falha
        else:
            if tipo_erro_anterior:
                print(f"[{hora()}] conexão restabelecida")
            tipo_erro_anterior = None
            if dados != anterior:
                resumo.mudancas += 1
                resumo.observar(dados)
                gravador.registrar("leitura", dados=dados)
                imprimir_mudancas(anterior, dados)
                anterior = dados

        # Mantém o ritmo fixo; se uma leitura atrasar, não tenta "compensar"
        proxima = max(proxima + intervalo, time.monotonic())
        time.sleep(max(0.0, proxima - time.monotonic()))


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # acentos no console do Windows

    parser = argparse.ArgumentParser(description="Grava o /dadosPlanta da Bancada Smart 4.0.")
    parser.add_argument("--url", default=URL_PADRAO, help=f"endereço da API (padrão: {URL_PADRAO})")
    parser.add_argument("--intervalo", type=float, default=1.0, help="segundos entre leituras (padrão: 1.0)")
    parser.add_argument("--timeout", type=float, default=2.0, help="timeout de cada leitura em s (padrão: 2.0)")
    parser.add_argument("--saida", type=Path, help="arquivo .jsonl (padrão: gravacoes/sessao_<data_hora>.jsonl)")
    args = parser.parse_args()

    caminho = args.saida or PASTA_GRAVACOES / f"sessao_{datetime.now():%Y%m%d_%H%M%S}.jsonl"
    gravador = Gravador(caminho)
    resumo = Resumo()
    gravador.registrar("inicio", url=args.url, intervalo_s=args.intervalo)

    print(f"Coletando {args.url} a cada {args.intervalo} s")
    print(f"Gravando em {caminho}")
    print("Ctrl+C para encerrar\n")

    try:
        coletar(args.url, args.intervalo, args.timeout, gravador, resumo)
    except KeyboardInterrupt:
        pass
    finally:
        gravador.registrar("fim", leituras=resumo.leituras, mudancas=resumo.mudancas)
        gravador.fechar()
        resumo.imprimir(caminho)


if __name__ == "__main__":
    main()

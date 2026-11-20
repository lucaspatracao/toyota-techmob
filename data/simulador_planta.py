"""
Simulador da Bancada Smart 4.0: responde GET /dadosPlanta igual ao Node-RED.

Mesma rota, mesmos campos, mesma ordem e mesmo formato do JSON da bancada:
o backend não sabe se está falando com a bancada ou com o simulador.
Basta trocar a URL. Dois modos:

  sintetico  gera pedidos a partir de ciclo_padrao.json (cronometragem da
             equipe), com variação de tempo, reprovação e paradas.
  replay     reproduz uma gravação REAL feita pelo coletor_planta.py.
             É o modo da apresentação, quando a bancada não está ligada.

Usa só a biblioteca padrão do Python: nada para instalar.

Uso:
    python simulador_planta.py                         # sintético, porta 1880
    python simulador_planta.py --velocidade 10         # 10x mais rápido
    python simulador_planta.py --modo replay --gravacao gravacoes/sessao_X.jsonl
Ctrl+C encerra.
"""
from __future__ import annotations

import argparse
import bisect
import json
import random
import sys
import threading
import time
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Callable, Protocol
from urllib.parse import urlsplit

PASTA = Path(__file__).resolve().parent
CONFIG_PADRAO = PASTA / "ciclo_padrao.json"
ROTA = "/dadosPlanta"
ESTACOES = ("estoque", "manipulacao", "separacao")
JANELA_15MIN_S = 15 * 60
ATRASO_PRIMEIRO_PEDIDO_S = 5.0


class FonteDados(Protocol):
    """Contrato comum dos dois modos: o estado da planta no instante t (s)."""

    def estado(self, t: float) -> dict: ...


def montar_estado(status_estacoes: dict[str, str], operacao_string: str,
                  aguardando: bool, produzindo: bool,
                  total: int, reprovados: int, ultimos_15min: int) -> dict:
    """Monta o JSON com os mesmos nomes e a mesma ordem do Node-RED da bancada."""
    return {
        "operacao_estoque": status_estacoes["estoque"],
        "operacao_manipulacao": status_estacoes["manipulacao"],
        "operacao_separacao": status_estacoes["separacao"],
        "operacao_string": operacao_string,
        "aguardando": int(aguardando),
        "produzindo": int(produzindo),
        "total_pedidos": total,
        "pedidos_reprovados": reprovados,
        "pedidos15min": ultimos_15min,
        "Temperatura": 0,  # a bancada não tem esse sensor: chega sempre 0
        "Umidade": 0,
    }


# --------------------------------------------------------------------------
# Modo sintético
# --------------------------------------------------------------------------

@dataclass
class Segmento:
    """Trecho de tempo em que UMA estação está em um status."""
    inicio: float
    fim: float
    estacao: str
    status: str


@dataclass
class Pedido:
    numero: int
    fim: float
    reprovado: bool


class SimuladorSintetico:
    """
    Gera uma linha do tempo de pedidos sob demanda. Cada pedido percorre as
    etapas de ciclo_padrao.json em sequência; o estado em um instante t é
    obtido por busca binária no segmento ativo. Os contadores (total,
    reprovados, últimos 15 min) consideram só os pedidos já concluídos em t.
    """

    def __init__(self, cfg: dict, seed: int | None = None) -> None:
        self.cfg = cfg
        self.status = cfg["status"]
        self.rng = random.Random(seed)
        self.segmentos: list[Segmento] = []
        self.inicios: list[float] = []  # paralelo a segmentos, para o bisect
        self.pedidos: list[Pedido] = []
        self.proximo_inicio = ATRASO_PRIMEIRO_PEDIDO_S
        self._lock = threading.Lock()  # o servidor atende requisições em paralelo

    def _variar(self, valor: float) -> float:
        v = self.cfg["variacao_pct"] / 100
        return valor * self.rng.uniform(1 - v, 1 + v)

    def _adicionar(self, inicio: float, duracao: float, estacao: str, status: str) -> float:
        self.segmentos.append(Segmento(inicio, inicio + duracao, estacao, status))
        self.inicios.append(inicio)
        return inicio + duracao

    def _gerar_pedido(self) -> None:
        operando, em_parada = self.status["operando"], self.status["em_parada"]
        t = inicio = self.proximo_inicio
        paradas = 0

        for etapa in self.cfg["etapas"]:
            estacao = etapa["estacao"]
            duracao = self._variar(etapa["duracao_s"])
            if self.rng.random() < self.cfg["chance_parada"]:
                # Parada no meio da etapa: opera, para, retoma
                antes = duracao * self.rng.uniform(0.2, 0.8)
                parada = self.rng.uniform(*self.cfg["duracao_parada_s"])
                t = self._adicionar(t, antes, estacao, operando)
                t = self._adicionar(t, parada, estacao, em_parada)
                t = self._adicionar(t, duracao - antes, estacao, operando)
                paradas += 1
            else:
                t = self._adicionar(t, duracao, estacao, operando)

        reprovado = self.rng.random() < self.cfg["chance_reprovacao"]
        pedido = Pedido(len(self.pedidos) + 1, t, reprovado)
        self.pedidos.append(pedido)
        self.proximo_inicio = t + self._variar(self.cfg["intervalo_entre_pedidos_s"])

        detalhes = [f"{t - inicio:.0f} s"]
        if paradas:
            detalhes.append(f"{paradas} parada(s)")
        if reprovado:
            detalhes.append("REPROVADO")
        print(f"[simulador] pedido {pedido.numero} programado: {', '.join(detalhes)}")

    def estado(self, t: float) -> dict:
        with self._lock:
            while self.proximo_inicio <= t:
                self._gerar_pedido()

            i = bisect.bisect_right(self.inicios, t) - 1
            ativo = self.segmentos[i] if i >= 0 and self.segmentos[i].fim > t else None

            concluidos = [p for p in self.pedidos if p.fim <= t]
            reprovados = sum(p.reprovado for p in concluidos)
            ultimos_15min = sum(p.fim > t - JANELA_15MIN_S for p in concluidos)

        status_estacoes = {e: self.status["parado"] for e in ESTACOES}
        if ativo:
            status_estacoes[ativo.estacao] = ativo.status
        produzindo = ativo is not None and ativo.status == self.status["operando"]

        return montar_estado(
            status_estacoes,
            operacao_string=ativo.status if ativo else self.status["parado"],
            aguardando=ativo is None,
            produzindo=produzindo,
            total=len(concluidos),
            reprovados=reprovados,
            ultimos_15min=ultimos_15min,
        )


# --------------------------------------------------------------------------
# Modo replay
# --------------------------------------------------------------------------

class Replay:
    """Reproduz as leituras de uma gravação do coletor, no tempo original."""

    PAUSA_FINAL_S = 5.0  # segura o último estado antes de recomeçar

    def __init__(self, caminho: Path, loop: bool) -> None:
        leituras: list[tuple[float, dict]] = []
        with caminho.open(encoding="utf-8") as arquivo:
            for linha in arquivo:
                if not linha.strip():
                    continue
                registro = json.loads(linha)
                if registro.get("tipo") == "leitura":
                    leituras.append((registro["t_s"], registro["dados"]))
        if not leituras:
            raise SystemExit(f"Nenhuma leitura encontrada em {caminho}")

        base = leituras[0][0]
        self.tempos = [t - base for t, _ in leituras]
        self.estados = [dados for _, dados in leituras]
        self.duracao = self.tempos[-1] + self.PAUSA_FINAL_S
        self.loop = loop
        print(f"[replay] {len(self.estados)} leituras, {self.duracao:.0f} s de gravação"
              f"{' (em loop)' if loop else ''}")

    def estado(self, t: float) -> dict:
        if self.loop:
            t %= self.duracao
        i = bisect.bisect_right(self.tempos, t) - 1
        return self.estados[max(i, 0)]


# --------------------------------------------------------------------------
# Servidor HTTP
# --------------------------------------------------------------------------

def criar_handler(fonte: FonteDados, relogio: Callable[[], float], verboso: bool):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self) -> None:
            if urlsplit(self.path).path != ROTA:
                self.send_error(404, f"Rota inexistente. Use {ROTA}")
                return
            # Mesmo formato do Node-RED: "chave":valor separados por ", "
            corpo = json.dumps(fonte.estado(relogio()), ensure_ascii=False,
                               separators=(", ", ":")).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(corpo)))
            self.end_headers()
            self.wfile.write(corpo)

        def log_message(self, formato: str, *args) -> None:
            if verboso:  # sem isso, cada consulta (1 por segundo) vira uma linha
                super().log_message(formato, *args)

    return Handler


def carregar_config(caminho: Path) -> dict:
    cfg = json.loads(caminho.read_text(encoding="utf-8"))
    for etapa in cfg["etapas"]:
        if etapa["estacao"] not in ESTACOES:
            raise SystemExit(f"Estação inválida em {caminho.name}: {etapa['estacao']} "
                             f"(use {', '.join(ESTACOES)})")
    return cfg


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # acentos no console do Windows

    parser = argparse.ArgumentParser(description="Simula o GET /dadosPlanta da Bancada Smart 4.0.")
    parser.add_argument("--modo", choices=("sintetico", "replay"), default="sintetico")
    parser.add_argument("--config", type=Path, default=CONFIG_PADRAO, help="tempos do modo sintético")
    parser.add_argument("--gravacao", type=Path, help="arquivo .jsonl do coletor (modo replay)")
    parser.add_argument("--loop", action=argparse.BooleanOptionalAction, default=True,
                        help="replay recomeça ao terminar (padrão: sim)")
    parser.add_argument("--velocidade", type=float, default=1.0, help="multiplicador do tempo (padrão: 1)")
    parser.add_argument("--seed", type=int, help="semente para repetir a mesma simulação")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--porta", type=int, default=1880, help="padrão 1880, igual ao Node-RED")
    parser.add_argument("--verboso", action="store_true", help="mostra cada requisição")
    args = parser.parse_args()

    if args.modo == "replay":
        if not args.gravacao:
            parser.error("--modo replay exige --gravacao <arquivo.jsonl>")
        fonte: FonteDados = Replay(args.gravacao, args.loop)
    else:
        fonte = SimuladorSintetico(carregar_config(args.config), args.seed)

    t0 = time.monotonic()

    def relogio() -> float:
        return (time.monotonic() - t0) * args.velocidade

    servidor = ThreadingHTTPServer((args.host, args.porta), criar_handler(fonte, relogio, args.verboso))
    print(f"Simulador ({args.modo}, {args.velocidade:g}x) em "
          f"http://{args.host}:{args.porta}{ROTA}  |  Ctrl+C para parar")
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        servidor.server_close()


if __name__ == "__main__":
    main()

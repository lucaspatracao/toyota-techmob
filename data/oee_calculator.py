"""
TechMob 4.0 - OEE por pedido a partir de uma gravação da Bancada Smart 4.0.

Lê uma gravação .jsonl do coletor_planta.py, identifica os pedidos com as
MESMAS regras do backend (pacote coleta) e calcula D, P, Q e OEE de cada
pedido. É a conferência do backend: para a mesma gravação, Python e Java
têm que dar o mesmo número.

Regras do pedido (docs/PLANO_ALINHAMENTO.md):
  1. Início: "aguardando" passa de 1 para 0 sem pedido aberto.
  2. Etapa: abre quando a estação sai de AGUARDANDO e fecha quando volta.
     Se a mesma estação voltar a operar no pedido, o tempo soma na mesma
     etapa. Tempo em status de parada (PARADO) conta como parada da etapa.
  3. Fim: "aguardando" volta para 1.
  4. Reprovado: "pedidos_reprovados" aumentou durante o pedido. Se o
     contador diminuir, é reinício da bancada, não reprovação.
  5. Interrompido: sem conexão por mais de 10 s com pedido aberto. O pedido
     fica fora do OEE.
O pedido está parado quando alguma estação está em PARADO ou quando
"aguardando" e "produzindo" estão os dois em 0. Cada intervalo entre duas
leituras usa o estado da leitura anterior (o dado só é gravado quando muda).

OEE do pedido (L = lead time, T_parado = soma das paradas,
T_padrão = soma do tempo padrão das etapas):
  D = (L - T_parado) / L
  P = mín(1; T_padrão / (L - T_parado))
  Q = 1 se aprovado, 0 se reprovado
  OEE = D x P x Q
Para vários pedidos, o OEE acumulado soma os tempos e aplica a mesma fórmula.

Uso:
    python oee_calculator.py gravacoes/sessao_20261117_101500.jsonl
    python oee_calculator.py gravacoes/sessao_X.jsonl --sem-csv
Saída: tabela no console e resultados/pedidos_<sessao>.csv
"""
from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from pathlib import Path

import pandas as pd

PASTA = Path(__file__).resolve().parent
CONFIG_PADRAO = PASTA / "ciclo_padrao.json"
PASTA_RESULTADOS = PASTA / "resultados"
ESTACOES = ("estoque", "manipulacao", "separacao")
SEGUNDOS_SEM_CONEXAO = 10.0

CONCLUIDO = "CONCLUIDO"
INTERROMPIDO = "INTERROMPIDO"
EM_ANDAMENTO = "EM_ANDAMENTO"


# --------------------------------------------------------------------------
# Cálculo do OEE
# --------------------------------------------------------------------------

@dataclass(frozen=True)
class ResultadoOEE:
    """Componentes do OEE em fração (0 a 1)."""
    disponibilidade: float
    performance: float
    qualidade: float

    @property
    def oee(self) -> float:
        return self.disponibilidade * self.performance * self.qualidade


def calcular_oee(lead_time_s: float, tempo_parado_s: float, tempo_padrao_s: float,
                 qualidade: float) -> ResultadoOEE:
    """D, P e Q a partir dos tempos. qualidade: 1 aprovado, 0 reprovado (ou fração)."""
    operando = lead_time_s - tempo_parado_s
    disponibilidade = operando / lead_time_s if lead_time_s > 0 else 0.0
    performance = min(1.0, tempo_padrao_s / operando) if operando > 0 else 0.0
    return ResultadoOEE(disponibilidade, performance, qualidade)


# --------------------------------------------------------------------------
# Pedido e etapa
# --------------------------------------------------------------------------

@dataclass
class Etapa:
    estacao: str
    inicio_s: float
    tempo_padrao_s: float
    fim_s: float | None = None
    duracao_s: float = 0.0
    tempo_parado_s: float = 0.0
    ativa: bool = True


@dataclass
class Pedido:
    numero: int
    inicio_s: float
    inicio_ts: str
    fim_s: float | None = None
    fim_ts: str | None = None
    situacao: str = EM_ANDAMENTO
    tempo_parado_s: float = 0.0
    reprovado: bool = False
    etapas: dict[str, Etapa] = field(default_factory=dict)

    @property
    def lead_time_s(self) -> float | None:
        return None if self.fim_s is None else self.fim_s - self.inicio_s

    @property
    def tempo_padrao_s(self) -> float:
        return sum(etapa.tempo_padrao_s for etapa in self.etapas.values())

    @property
    def oee(self) -> ResultadoOEE | None:
        """Só pedido concluído tem OEE; interrompido fica de fora."""
        if self.situacao != CONCLUIDO:
            return None
        return calcular_oee(self.lead_time_s, self.tempo_parado_s, self.tempo_padrao_s,
                            0.0 if self.reprovado else 1.0)


def oee_acumulado(pedidos: list[Pedido]) -> ResultadoOEE | None:
    """Soma os tempos dos pedidos concluídos e aplica a fórmula uma vez."""
    concluidos = [p for p in pedidos if p.situacao == CONCLUIDO]
    if not concluidos:
        return None
    aprovados = sum(not p.reprovado for p in concluidos)
    return calcular_oee(
        sum(p.lead_time_s for p in concluidos),
        sum(p.tempo_parado_s for p in concluidos),
        sum(p.tempo_padrao_s for p in concluidos),
        aprovados / len(concluidos),
    )


# --------------------------------------------------------------------------
# Detector de pedidos (mesmas regras do DetectorPedidos do backend)
# --------------------------------------------------------------------------

class DetectorPedidos:
    """
    Recebe as leituras em ordem e guarda os pedidos fechados em `fechados`.

    O coletor só grava quando o dado muda, então o estado de uma leitura vale
    até a próxima. Uma queda começa no primeiro registro "erro"; a última
    leitura boa foi um intervalo de coleta antes dele. É a mesma conta do
    backend, que consulta a bancada a cada intervalo.
    """

    def __init__(self, tempo_padrao_s: dict[str, float], status_parado: str = "AGUARDANDO",
                 status_parada: str = "PARADO",
                 segundos_sem_conexao: float = SEGUNDOS_SEM_CONEXAO,
                 intervalo_s: float = 1.0) -> None:
        self.tempo_padrao_s = tempo_padrao_s
        self.status_parado = status_parado
        self.status_parada = status_parada
        self.segundos_sem_conexao = segundos_sem_conexao
        self.intervalo_s = intervalo_s
        self.fechados: list[Pedido] = []
        self.pedido: Pedido | None = None
        self.anterior: dict | None = None
        self.t_anterior = 0.0
        self.queda_t: float | None = None
        self.queda_ts = ""
        self._numero = 0
        self._reprovados_ref: int | None = None

    # ---- entrada ---------------------------------------------------------

    def leitura(self, dados: dict, t: float, ts: str = "") -> None:
        self._verificar_queda(t)
        if self.pedido is not None and self.anterior is not None:
            self._acumular(self.anterior, t - self.t_anterior)

        if self.pedido is None:
            if (self.anterior is not None and self.anterior.get("aguardando") == 1
                    and dados.get("aguardando") == 0):
                self._iniciar(dados, t, ts)
        else:
            self._atualizar_reprovado(dados)
            self._transicoes(dados, t)
            if dados.get("aguardando") == 1:
                self._fechar(t, ts, CONCLUIDO)

        self.anterior, self.t_anterior = dados, t

    def erro(self, t: float, ts: str = "") -> None:
        """Falha de leitura (sem conexão ou JSON inválido): marca o início da queda."""
        if self.queda_t is None:
            self.queda_t, self.queda_ts = t, ts

    def encerrar(self, t: float) -> None:
        """Fim da gravação. Pedido ainda aberto fica como EM_ANDAMENTO (sem OEE)."""
        self._verificar_queda(t)
        if self.pedido is not None:
            self.fechados.append(self.pedido)
            self.pedido = None
        self.anterior = None

    # ---- regras ------------------------------------------------------------

    def _verificar_queda(self, t: float) -> None:
        """Regra 5: mais de N s sem leitura boa interrompe o pedido aberto."""
        if self.queda_t is None:
            return
        queda_t, queda_ts = self.queda_t, self.queda_ts
        self.queda_t, self.queda_ts = None, ""
        if t - queda_t <= self.segundos_sem_conexao:
            return  # queda curta: o pedido continua
        if self.pedido is not None:
            # O pedido termina na última leitura boa, um intervalo antes da queda.
            fim = max(self.t_anterior, queda_t - self.intervalo_s)
            if self.anterior is not None:
                self._acumular(self.anterior, fim - self.t_anterior)
            self._fechar(fim, _menos_segundos(queda_ts, queda_t - fim), INTERROMPIDO)
        self.anterior = None  # depois da queda, só abre pedido com nova transição

    def _iniciar(self, dados: dict, t: float, ts: str) -> None:
        self._numero += 1
        self.pedido = Pedido(self._numero, t, ts)
        self._reprovados_ref = dados.get("pedidos_reprovados")
        self._transicoes(dados, t)

    def _acumular(self, anterior: dict, dt: float) -> None:
        if self._em_parada(anterior):
            self.pedido.tempo_parado_s += dt
        for estacao, etapa in self.pedido.etapas.items():
            if etapa.ativa:
                etapa.duracao_s += dt
                if anterior.get(f"operacao_{estacao}") == self.status_parada:
                    etapa.tempo_parado_s += dt

    def _em_parada(self, leitura: dict) -> bool:
        if any(leitura.get(f"operacao_{e}") == self.status_parada for e in ESTACOES):
            return True
        return leitura.get("aguardando") == 0 and leitura.get("produzindo") == 0

    def _atualizar_reprovado(self, dados: dict) -> None:
        atual = dados.get("pedidos_reprovados")
        if atual is None:
            return
        if self._reprovados_ref is not None and atual > self._reprovados_ref:
            self.pedido.reprovado = True
        self._reprovados_ref = atual  # se diminuiu, é reinício: só acompanha

    def _transicoes(self, dados: dict, t: float) -> None:
        for estacao in ESTACOES:
            status = dados.get(f"operacao_{estacao}")
            ociosa = status is None or status == self.status_parado
            etapa = self.pedido.etapas.get(estacao)
            if not ociosa and (etapa is None or not etapa.ativa):
                if etapa is None:
                    etapa = Etapa(estacao, t, self.tempo_padrao_s.get(estacao, 0.0))
                    self.pedido.etapas[estacao] = etapa
                etapa.ativa = True
            elif ociosa and etapa is not None and etapa.ativa:
                etapa.ativa = False
                etapa.fim_s = t

    def _fechar(self, t: float, ts: str, situacao: str) -> None:
        pedido = self.pedido
        for etapa in pedido.etapas.values():
            if etapa.ativa:
                etapa.ativa = False
                etapa.fim_s = t
        pedido.fim_s, pedido.fim_ts, pedido.situacao = t, ts, situacao
        self.fechados.append(pedido)
        self.pedido = None


def _menos_segundos(ts: str, segundos: float) -> str:
    """Horário ISO menos alguns segundos (para o fim do pedido interrompido)."""
    try:
        return (datetime.fromisoformat(ts) - timedelta(seconds=segundos)).isoformat(timespec="milliseconds")
    except ValueError:
        return ts


# --------------------------------------------------------------------------
# Leitura da gravação
# --------------------------------------------------------------------------

def carregar_config(caminho: Path = CONFIG_PADRAO) -> dict:
    return json.loads(Path(caminho).read_text(encoding="utf-8"))


def criar_detector(cfg: dict, segundos_sem_conexao: float = SEGUNDOS_SEM_CONEXAO,
                   intervalo_s: float = 1.0) -> DetectorPedidos:
    return DetectorPedidos(
        {etapa["estacao"]: float(etapa["duracao_s"]) for etapa in cfg["etapas"]},
        status_parado=cfg["status"]["parado"],
        status_parada=cfg["status"]["em_parada"],
        segundos_sem_conexao=segundos_sem_conexao,
        intervalo_s=intervalo_s,
    )


def ler_gravacao(caminho: Path) -> list[dict]:
    """Registros do coletor, na ordem em que foram gravados."""
    registros = []
    with Path(caminho).open(encoding="utf-8") as arquivo:
        for numero, linha in enumerate(arquivo, start=1):
            if not linha.strip():
                continue
            try:
                registros.append(json.loads(linha))
            except json.JSONDecodeError as exc:
                raise ValueError(f"{caminho}: linha {numero} não é JSON válido") from exc
    return registros


def detectar_pedidos(registros: list[dict], cfg: dict,
                     segundos_sem_conexao: float = SEGUNDOS_SEM_CONEXAO) -> list[Pedido]:
    """Passa a gravação pelo detector. Cada "inicio" é uma nova sessão do coletor."""
    pedidos: list[Pedido] = []
    detector = criar_detector(cfg, segundos_sem_conexao)
    for registro in registros:
        tipo, t, ts = registro.get("tipo"), float(registro.get("t_s", 0.0)), registro.get("ts", "")
        if tipo == "inicio":
            # Nova sessão no mesmo arquivo: o relógio (t_s) recomeça do zero.
            detector.encerrar(detector.t_anterior)
            pedidos.extend(detector.fechados)
            detector = criar_detector(cfg, segundos_sem_conexao, float(registro.get("intervalo_s", 1.0)))
        elif tipo == "leitura":
            detector.leitura(registro["dados"], t, ts)
        elif tipo == "erro":
            detector.erro(t, ts)
        elif tipo == "fim":
            detector.encerrar(t)
    detector.encerrar(detector.t_anterior)
    pedidos.extend(detector.fechados)
    # Numeração única no arquivo inteiro, na ordem de início.
    for numero, pedido in enumerate(pedidos, start=1):
        pedido.numero = numero
    return pedidos


# --------------------------------------------------------------------------
# Saída
# --------------------------------------------------------------------------

def tabela_pedidos(pedidos: list[Pedido]) -> pd.DataFrame:
    linhas = []
    for p in pedidos:
        r = p.oee
        linha = {
            "numero": p.numero,
            "inicio": p.inicio_ts,
            "fim": p.fim_ts,
            "situacao": p.situacao,
            "lead_time_s": p.lead_time_s,
            "tempo_parado_s": p.tempo_parado_s,
            "tempo_padrao_s": p.tempo_padrao_s,
            "reprovado": p.reprovado,
        }
        for estacao in ESTACOES:
            etapa = p.etapas.get(estacao)
            linha[f"{estacao}_s"] = etapa.duracao_s if etapa else None
        linha.update({
            "disponibilidade": r.disponibilidade * 100 if r else None,
            "performance": r.performance * 100 if r else None,
            "qualidade": r.qualidade * 100 if r else None,
            "oee": r.oee * 100 if r else None,
        })
        linhas.append(linha)
    colunas = ["numero", "inicio", "fim", "situacao", "lead_time_s", "tempo_parado_s",
               "tempo_padrao_s", "reprovado", *(f"{e}_s" for e in ESTACOES),
               "disponibilidade", "performance", "qualidade", "oee"]
    return pd.DataFrame(linhas, columns=colunas).round(3)


def imprimir(pedidos: list[Pedido], caminho: Path) -> None:
    print(f"Gravação: {caminho}")
    if not pedidos:
        print("Nenhum pedido encontrado.")
        return
    tabela = tabela_pedidos(pedidos)
    exibicao = tabela.drop(columns=["fim"]).copy()
    exibicao["inicio"] = exibicao["inicio"].str.slice(11, 19)
    print(exibicao.to_string(index=False, na_rep="-", float_format=lambda v: f"{v:.1f}"))

    concluidos = [p for p in pedidos if p.situacao == CONCLUIDO]
    reprovados = sum(p.reprovado for p in concluidos)
    interrompidos = sum(p.situacao == INTERROMPIDO for p in pedidos)
    print(f"\nPedidos concluídos: {len(concluidos)} (reprovados: {reprovados}), "
          f"interrompidos: {interrompidos}")
    total = oee_acumulado(pedidos)
    if total:
        print(f"OEE acumulado (soma dos tempos): {total.oee * 100:.1f}%  "
              f"D {total.disponibilidade * 100:.1f}%  P {total.performance * 100:.1f}%  "
              f"Q {total.qualidade * 100:.1f}%")


def salvar_csv(pedidos: list[Pedido], caminho: Path, pasta: Path = PASTA_RESULTADOS) -> Path:
    sessao = Path(caminho).stem.removeprefix("sessao_")
    destino = Path(pasta) / f"pedidos_{sessao}.csv"
    destino.parent.mkdir(parents=True, exist_ok=True)
    tabela_pedidos(pedidos).to_csv(destino, index=False)
    return destino


def main(argv: list[str] | None = None) -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # acentos no console do Windows

    parser = argparse.ArgumentParser(description="OEE por pedido a partir de uma gravação da bancada.")
    parser.add_argument("gravacao", type=Path, help="arquivo .jsonl do coletor_planta.py")
    parser.add_argument("--config", type=Path, default=CONFIG_PADRAO, help="tempo padrão e textos de status")
    parser.add_argument("--segundos-sem-conexao", type=float, default=SEGUNDOS_SEM_CONEXAO,
                        help="tempo sem leitura que interrompe o pedido (padrão: 10)")
    parser.add_argument("--saida", type=Path, default=PASTA_RESULTADOS, help="pasta do CSV (padrão: resultados/)")
    parser.add_argument("--sem-csv", action="store_true", help="só mostra a tabela no console")
    args = parser.parse_args(argv)

    pedidos = detectar_pedidos(ler_gravacao(args.gravacao), carregar_config(args.config),
                               args.segundos_sem_conexao)
    imprimir(pedidos, args.gravacao)
    if pedidos and not args.sem_csv:
        print(f"\nCSV: {salvar_csv(pedidos, args.gravacao, args.saida)}")


if __name__ == "__main__":
    main()

# Python — coleta, simulação e análise

O Python não fica no caminho ao vivo: quem lê a bancada, calcula o OEE e grava no MySQL é o backend. Aqui ficam três papéis:

| Papel | Arquivo | Precisa de |
|---|---|---|
| Coletar a bancada | `coletor_planta.py` | Só a biblioteca padrão |
| Simular a bancada (sintético ou replay) | `simulador_planta.py` | Só a biblioteca padrão |
| Analisar as gravações | `oee_calculator.py`, `analisar_cronometragem.py` | pandas |

```powershell
cd data
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements-dev.txt     # pandas + pytest (requirements.txt tem só o pandas)
```

## Coletor: `coletor_planta.py`

Lê o `GET /dadosPlanta` do Node-RED da bancada **sem alterar nada nela** e grava só as mudanças, com o horário, em `gravacoes/sessao_<data_hora>.jsonl`.

```powershell
python coletor_planta.py --url http://<IP do notebook do laboratório>:1880/dadosPlanta
```

| Opção | Padrão | Para quê |
|---|---|---|
| `--url` | `http://localhost:1880/dadosPlanta` | Endereço do Node-RED (ou do simulador) |
| `--intervalo` | `1.0` | Segundos entre leituras |
| `--timeout` | `2.0` | Tempo máximo de cada leitura |
| `--saida` | `gravacoes/sessao_<data_hora>.jsonl` | Arquivo de saída |

`Ctrl+C` encerra e mostra o **resumo**: quantas leituras, quantas mudanças, os erros e todos os valores que apareceram em cada campo. Esse resumo é a lista real dos textos de status da bancada.

Cada linha da gravação é um JSON:

```json
{"tipo": "inicio",  "ts": "2026-11-17T10:00:00.000", "t_s": 0.0, "url": "http://...", "intervalo_s": 1.0}
{"tipo": "leitura", "ts": "2026-11-17T10:00:10.500", "t_s": 10.5, "dados": { ...JSON do /dadosPlanta... }}
```

Falhas de leitura também são gravadas (com o texto bruto, quando o Node-RED devolve JSON inválido). **As gravações reais em `gravacoes/` são versionadas**: são a evidência do projeto e não têm dado pessoal.

## Simulador: `simulador_planta.py`

Responde `GET /dadosPlanta` com o mesmo JSON, na mesma ordem de campos, do Node-RED da bancada. Para o backend, trocar a bancada pelo simulador é só trocar a URL.

```powershell
python simulador_planta.py                                               # sintético, porta 1880
python simulador_planta.py --modo replay --gravacao gravacoes/<sessao>.jsonl
python simulador_planta.py --porta 1881                                  # reserva (fallback) da bancada
```

| Modo | O que faz | Origem gravada no pedido |
|---|---|---|
| `sintetico` | Gera pedidos a partir do `ciclo_padrao.json`, com variação de tempo, paradas e reprovação | `SINTETICO` |
| `replay` | Reproduz uma gravação real do coletor (recomeça ao terminar; `--no-loop` para parar) | `REPLAY` |

Cada resposta leva o cabeçalho `X-TechMob-Fonte: sintetico` ou `replay`, e é assim que o backend grava a origem do pedido. O Node-RED real não envia o cabeçalho: sem ele, a origem é `BANCADA`.

Outras opções: `--seed` (repete a mesma simulação), `--velocidade` (multiplica o tempo), `--host`, `--verboso`.

> **Cuidado com `--velocidade`.** O backend mede o tempo pelo relógio real. A 4x, um pedido de 80 s dura 20 s e a performance fica sempre em 100%. Para OEE realista, use velocidade 1, ou divida também o tempo padrão do backend (`TEMPO_PADRAO_*_S`) pelo mesmo fator.

### `ciclo_padrao.json`

Tempo padrão de cada etapa (cronometragem), textos de status e parâmetros do modo sintético. Os tempos atuais são **provisórios**: 20 s estoque, 35 s manipulação, 25 s separação, 80 s por pedido. Precisam ser os mesmos do backend (`TEMPO_PADRAO_*_S`) e do seed do `infra/schema.sql`.

## Análise

### `oee_calculator.py`: OEE por pedido de uma gravação

Identifica os pedidos de uma gravação com as **mesmas regras do backend** e calcula D, P, Q e OEE de cada um, mais o acumulado. É a conferência: para a mesma gravação, Python e Java têm que dar o mesmo número.

```powershell
python oee_calculator.py gravacoes/<sessao>.jsonl            # tabela no console + resultados/pedidos_<sessao>.csv
python oee_calculator.py gravacoes/<sessao>.jsonl --sem-csv
```

A pasta `resultados/` é gerada e não é versionada.

### `analisar_cronometragem.py`: tempo padrão real e gargalo

Lê uma ou mais gravações reais e, com os pedidos concluídos:

1. calcula o tempo padrão de cada etapa pela **mediana** do tempo operando (duração menos paradas), que não se deixa levar por um pedido fora da curva;
2. aponta o gargalo: a etapa com maior variação e a com maior atraso sobre o padrão atual.

```powershell
python analisar_cronometragem.py                      # todas as gravações de gravacoes/
python analisar_cronometragem.py gravacoes/ --atualizar   # grava as medianas no ciclo_padrao.json
```

Depois de `--atualizar`, leve os mesmos valores para o backend (`TEMPO_PADRAO_*_S`) e para o seed do `schema.sql`.

## Como um pedido é identificado

O JSON da bancada é uma foto do momento, não um evento: o pedido é descoberto pelas mudanças entre leituras. As regras são as mesmas no Python (`oee_calculator.py`) e no backend (`DetectorPedidos`):

1. **Início:** `aguardando` passa de 1 para 0 sem pedido aberto.
2. **Etapa:** abre quando a estação sai de `AGUARDANDO` e fecha quando volta. Tempo com a estação em parada (`PARADO`) conta como parada da etapa.
3. **Fim:** `aguardando` volta para 1. O pedido é fechado e o OEE é calculado.
4. **Reprovado:** `pedidos_reprovados` aumentou entre o início e o fim. Se o contador diminuir, é reinício da bancada, não reprovação.
5. **Interrompido:** sem leitura boa por mais de 10 s com pedido aberto. O pedido fica gravado como `INTERROMPIDO` e fora do OEE.

O pedido também conta como parado quando `aguardando` e `produzindo` estão os dois em 0.

## OEE por pedido

L = lead time, T_parado = soma das paradas, T_padrão = soma do tempo padrão das etapas:

- D = (L − T_parado) ÷ L
- P = mín(1; T_padrão ÷ (L − T_parado))
- Q = 1 se aprovado, 0 se reprovado
- OEE = D × P × Q

Exemplo: padrão de 80 s, pedido de 100 s com 12 s parado → D = 88%, P = 80 ÷ 88 = 90,9%, Q = 100%, **OEE = 80,0%** (é a gravação `tests/gravacoes/exemplo_80.jsonl`).

Para vários pedidos, o acumulado soma os tempos de todos e aplica a mesma fórmula; Q vira aprovados ÷ concluídos. A média dos OEEs distorceria quando os pedidos têm durações diferentes.

## Testes

```powershell
pytest
```

Os testes (`tests/`) usam duas gravações pequenas: `exemplo_80.jsonl` (o exemplo acima) e `sintetica_seed12.jsonl` (simulador com semente 12). Cobrem pedidos detectados, reprovado, interrompido, o cabeçalho `X-TechMob-Fonte` e a cronometragem. O backend usa as mesmas gravações e confere os mesmos números.

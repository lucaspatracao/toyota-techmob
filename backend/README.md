# Backend TechMob 4.0

Backend em Java (Spring Boot) com dois papéis:

1. **Coleta (pacote `coleta`):** consulta o `GET /dadosPlanta` da Bancada Smart 4.0 a cada 1 s, identifica cada pedido pelas mudanças de estado, calcula o OEE do pedido e grava pedido, etapas e indicador no MySQL. É a única parte do sistema que escreve no banco.
2. **API REST:** entrega ao dashboard o estado ao vivo da bancada, os pedidos e o OEE. Todos os endpoints são `GET`.

```
Node-RED da bancada (ou simulador) → PlantaClient → DetectorPedidos → CalculadoraOEE → MySQL
                                                       └─ estado ao vivo em memória ─┐   │
                                                                                     ▼   ▼
                                                                         API REST → Dashboard
```

## Requisitos

- Java 25+
- MySQL 8 com o schema de [`infra/schema.sql`](../infra/schema.sql)
- Maven não precisa ser instalado: o projeto usa o wrapper `mvnw`

## Variáveis de ambiente

As credenciais do banco vêm do ambiente, nunca do repositório. Só `DB_PASSWORD` é obrigatória; as demais têm valor padrão.

| Variável | Padrão |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `techmob` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(obrigatória)* |
| `SHOW_SQL` | `false` (use `true` para ver o SQL no console) |

A coleta (propriedades `techmob.*` em `application.properties`, lidas pelo `PlantaProperties`) também tem uma variável e um padrão para cada valor:

| Variável | Propriedade | Padrão | Para quê |
|---|---|---|---|
| `COLETA_HABILITADA` | `techmob.coleta.habilitada` | `true` | `false` desliga a leitura da bancada (os testes rodam assim) |
| `MAQUINA_ID` | `techmob.planta.maquina-id` | `1` | Máquina que recebe os pedidos |
| `PLANTA_URL` | `techmob.planta.url` | `http://localhost:1880/dadosPlanta` | Node-RED da bancada (ou o simulador no lugar dela) |
| `PLANTA_URL_FALLBACK` | `techmob.planta.url-fallback` | `http://localhost:1881/dadosPlanta` | Simulador lido depois de 3 falhas seguidas na `PLANTA_URL` |
| `PLANTA_INTERVALO_MS` | `techmob.planta.intervalo-ms` | `1000` | Intervalo entre leituras |
| `PLANTA_TIMEOUT_MS` | `techmob.planta.timeout-ms` | `2000` | Tempo máximo de cada leitura |
| `PLANTA_SEGUNDOS_SEM_CONEXAO` | `techmob.planta.segundos-sem-conexao` | `10` | Sem leitura boa por mais que isso, o pedido aberto vira `INTERROMPIDO` |
| `PLANTA_STATUS_PARADO` | `techmob.planta.status-parado` | `AGUARDANDO` | Texto da estação ociosa (o lido na bancada) |
| `PLANTA_STATUS_PARADA` | `techmob.planta.status-parada` | `PARADO` | Texto da estação em parada (o do simulador) |
| `META_OEE` | `techmob.planta.meta-oee` | `75` | Meta de OEE em % |
| `TEMPO_PADRAO_ESTOQUE_S`, `TEMPO_PADRAO_MANIPULACAO_S`, `TEMPO_PADRAO_SEPARACAO_S` | `techmob.planta.tempo-padrao-s.*` | `20`, `35`, `25` | Cronometragem de cada estação (a mesma do `data/ciclo_padrao.json`) |

## Execução local

### Opção 1: comando direto

```powershell
cd backend
$env:DB_PASSWORD = "sua-senha"
.\mvnw.cmd spring-boot:run
```

### Opção 2: atalho do projeto

```powershell
cd backend
.\run-local.cmd
```

O atalho pede a senha se `DB_PASSWORD` não estiver definida.

A API sobe em http://localhost:8080. Para conferir se ela e o banco estão no ar: http://localhost:8080/actuator/health

Ao subir, a coleta já começa a ler a `PLANTA_URL`. O log mostra cada pedido:

```
Coleta ligada: lendo http://localhost:1880/dadosPlanta a cada 1000 ms (fallback: http://localhost:1881/dadosPlanta)
Pedido 12 iniciado (SINTETICO)
Pedido 12 concluído em 97 s: OEE 80.000%
```

Pedidos que ficaram `EM_ANDAMENTO` porque o backend parou no meio viram `INTERROMPIDO` na próxima subida.

### Fonte de dados: o simulador (padrão)

Em outro terminal, suba o simulador na porta padrão da bancada; o backend não precisa de nenhuma variável a mais:

```powershell
python data/simulador_planta.py                                            # pedidos sintéticos
python data/simulador_planta.py --modo replay --gravacao data/gravacoes/<sessao>.jsonl
```

Com a bancada real ligada, aponte para ela e deixe o simulador de reserva na 1881:

```powershell
$env:PLANTA_URL = "http://<IP do Node-RED da bancada>:1880/dadosPlanta"
python data/simulador_planta.py --modo replay --gravacao data/gravacoes/<sessao>.jsonl --porta 1881
```

## Testes

```powershell
.\mvnw.cmd test
```

Não precisam de MySQL nem da bancada:

- **Regras da coleta** (`coleta/*Test`): `DetectorPedidosTest` e `CalculadoraOEETest` usam as mesmas gravações do Python (`src/test/resources/gravacoes/`) e conferem os mesmos números (o exemplo de 80,0%, reprovado, interrompido, performance limitada a 100%, acumulado por soma de tempos). `PlantaClientTest` cobre resposta normal, JSON inválido, queda e troca para o fallback; `ColetorPlantaTest` passa uma gravação inteira pelo laço da coleta.
- **Testes de service** (`service/*Test`): usam mocks dos repositórios.
- **`ApiContratoIntegrationTest`**: sobe a aplicação inteira com H2 em memória e relógio fixo, chama cada endpoint por HTTP e confere o JSON exato, além dos erros 404, 400 e 405 e do CORS. Se algum campo mudar de nome ou formato, o teste falha antes de quebrar o frontend.

## Estrutura

```
src/main/java/com/toyota/techmob/backend/
├── coleta/      leitura da bancada: PlantaClient (GET com timeout e fallback),
│                DetectorPedidos e CalculadoraOEE (classes puras, sem Spring),
│                ColetorPlanta (@Scheduled) e EstadoPlantaAtual (estado ao vivo em memória)
├── controller/  recebe as requisições HTTP e devolve JSON
├── service/     regras de negócio (interface + implementação); RegistroPedidoService é o único que grava
├── repository/  acesso ao banco (Spring Data JPA)
├── domain/      entidades JPA, espelham as tabelas
├── dto/         respostas da API (records); a entidade nunca é exposta direto
├── mapper/      conversão entidade → DTO, num lugar só
├── exception/   tratamento global de erros
└── config/      PlantaProperties (techmob.planta.*) e CORS (só GET, para localhost:5173 e :3000)
```

Caminho de uma requisição: `Controller → Service → Repository → MySQL`, e na volta `Entidade → DtoMapper → DTO → JSON`. O estado ao vivo (`/api/planta/{id}/status`) vem da memória da coleta, sem passar pelo banco.

Caminho de uma leitura da bancada: `ColetorPlanta → PlantaClient → DetectorPedidos → (eventos) → RegistroPedidoService → MySQL`.

## Endpoints

Todos são `GET`. O contrato segue o plano de alinhamento (`docs/PLANO_ALINHAMENTO.md`): o OEE é **por pedido** da bancada (3 peças), não por turno.

| Rota | Retorna |
|---|---|
| `/api/maquinas` | Lista de máquinas |
| `/api/dashboard/{maquinaId}` | Máquina, OEE do último pedido, estado ao vivo, resumo de hoje e meta |
| `/api/planta/{maquinaId}/status` | Estado ao vivo: estações, pedido em andamento, fonte e conexão |
| `/api/pedidos/{maquinaId}?de=&ate=&origem=` | Pedidos do período com OEE, do mais recente ao mais antigo |
| `/api/pedidos/detalhe/{pedidoId}` | Um pedido com as etapas e o tempo padrão |
| `/api/indicadores/historico/{maquinaId}` | OEE pedido a pedido, do mais recente ao mais antigo |
| `/actuator/health` | Situação da API e do banco |

Convenções do JSON:

- Datas em ISO-8601 UTC (`2026-11-17T13:05:12.418Z`).
- Tempos em segundos (`leadTimeS`, `tempoParadoS`, `tempoPadraoS`...).
- Percentuais de 0 a 100 (`disponibilidade`, `performance`, `qualidade`, `oee`).
- Estações: `ESTOQUE`, `MANIPULACAO`, `SEPARACAO`. Origem do pedido: `BANCADA`, `REPLAY`, `SINTETICO`. Situação do pedido: `EM_ANDAMENTO`, `CONCLUIDO`, `INTERROMPIDO`. Status da máquina: `AGUARDANDO`, `PRODUZINDO`, `PARADO`, `SEM_CONEXAO`.

### Exemplos de resposta

Os números abaixo usam os tempos do projeto (padrão de 80 s por pedido) e o pedido do exemplo do plano: 100 s de lead time com 12 s parado, OEE de 80%.

`GET /api/maquinas`

```json
[
  { "id": 1, "bancadaId": "BANCADA_SMART_01", "nome": "Bancada Smart 4.0 - Lab SENAI",
    "statusAtual": "PRODUZINDO", "ultimaLeituraEm": "2026-11-17T13:05:12.418Z" }
]
```

`GET /api/planta/1/status`

```json
{
  "maquinaId": 1,
  "status": "PRODUZINDO",
  "conectado": true,
  "fonte": "REPLAY",
  "operacao": "PRODUZINDO",
  "estacoes": [
    { "estacao": "ESTOQUE", "status": "AGUARDANDO", "situacao": "AGUARDANDO", "tempoPadraoS": 20.0 },
    { "estacao": "MANIPULACAO", "status": "PRODUZINDO", "situacao": "OPERANDO", "tempoPadraoS": 35.0 },
    { "estacao": "SEPARACAO", "status": "AGUARDANDO", "situacao": "AGUARDANDO", "tempoPadraoS": 25.0 }
  ],
  "pedidoEmAndamento": {
    "pedidoId": 43, "numero": 43, "inicio": "2026-11-17T13:04:40.102Z",
    "decorridoS": 32.3, "etapaAtual": "MANIPULACAO"
  },
  "tempoPadraoPedidoS": 80.0,
  "ultimaLeituraEm": "2026-11-17T13:05:12.418Z"
}
```

- `status` da estação é o texto que a bancada envia; `situacao` é a leitura dele (`OPERANDO`, `AGUARDANDO`, `PARADO` ou `SEM_CONEXAO`).
- `fonte` diz de onde vem o dado: `BANCADA` (Node-RED real, sem cabeçalho) ou `REPLAY`/`SINTETICO` (simulador, cabeçalho `X-TechMob-Fonte`). Vem `null` antes da primeira leitura.
- Sem conexão com a bancada: `"status": "SEM_CONEXAO"`, `"conectado": false`, todas as estações com `"situacao": "SEM_CONEXAO"`.
- `pedidoEmAndamento` vem `null` quando a bancada está aguardando pedido.

`GET /api/dashboard/1`

```json
{
  "maquina": { "id": 1, "bancadaId": "BANCADA_SMART_01", "nome": "Bancada Smart 4.0 - Lab SENAI",
               "statusAtual": "PRODUZINDO", "ultimaLeituraEm": "2026-11-17T13:05:12.418Z" },
  "indicadorAtual": {
    "maquinaId": 1, "pedidoId": 42,
    "disponibilidade": 88.0, "performance": 90.909, "qualidade": 100.0, "oee": 80.0,
    "periodoInicio": "2026-11-17T13:02:10.250Z", "periodoFim": "2026-11-17T13:03:50.250Z",
    "calculadoEm": "2026-11-17T13:03:50.250Z"
  },
  "planta": { "maquinaId": 1, "status": "PRODUZINDO", "conectado": true, "fonte": "REPLAY" },
  "resumoHoje": {
    "concluidos": 12, "aprovados": 11, "reprovados": 1, "interrompidos": 0, "pecasProduzidas": 36,
    "taxaReprovacao": 8.333, "leadTimeMedioS": 97.4, "tempoParadoTotalS": 103.2,
    "disponibilidade": 91.17, "performance": 90.09, "qualidade": 91.667, "oee": 75.291
  },
  "metaOee": 75.0
}
```

- `planta` é o mesmo objeto de `/api/planta/1/status` (resumido acima).
- `indicadorAtual` é o OEE do último pedido concluído; vem `null` enquanto não houver nenhum.
- `resumoHoje` vai da meia-noite à meia-noite no fuso do servidor (o de Brasília) e conta só pedidos concluídos (os interrompidos aparecem à parte). O OEE do resumo **soma os tempos** de todos os pedidos e aplica a fórmula uma vez; não é a média dos OEEs. Sem pedido concluído no dia, as médias, a taxa e o OEE vêm `null`.
- `metaOee` vem da variável `META_OEE` (padrão 75).

`GET /api/pedidos/1?de=2026-11-17T03:00:00Z&ate=2026-11-18T03:00:00Z&origem=REPLAY`

```json
[
  {
    "id": 42, "maquinaId": 1, "numero": 42, "origem": "REPLAY", "situacao": "CONCLUIDO",
    "inicio": "2026-11-17T13:02:10.250Z", "fim": "2026-11-17T13:03:50.250Z",
    "leadTimeS": 100.0, "tempoParadoS": 12.0, "tempoPadraoS": 80.0,
    "reprovado": false, "pecasBoas": 3, "pecasDefeituosas": 0,
    "duracaoEtapasS": { "ESTOQUE": 21.0, "MANIPULACAO": 47.0, "SEPARACAO": 26.0 },
    "disponibilidade": 88.0, "performance": 90.909, "qualidade": 100.0, "oee": 80.0
  },
  {
    "id": 41, "maquinaId": 1, "numero": 41, "origem": "REPLAY", "situacao": "INTERROMPIDO",
    "inicio": "2026-11-17T12:58:02Z", "fim": "2026-11-17T12:58:40Z",
    "leadTimeS": 38.0, "tempoParadoS": 0.0, "tempoPadraoS": 55.0,
    "reprovado": false, "pecasBoas": 0, "pecasDefeituosas": 0,
    "duracaoEtapasS": { "ESTOQUE": 20.0, "MANIPULACAO": 18.0 },
    "disponibilidade": null, "performance": null, "qualidade": null, "oee": null
  }
]
```

- `de` e `ate`: instantes ISO-8601, opcionais e independentes (sem eles, todos os pedidos). O filtro é pelo início do pedido, no intervalo `[de, ate)`. Aceitam fuso: `de=2026-11-17T00:00:00-03:00` é o mesmo que `de=2026-11-17T03:00:00Z` (um fuso positivo precisa de `%2B` no lugar do `+` na URL).
- `origem`: opcional; sem ela, todas as origens.
- `duracaoEtapasS` traz o tempo de cada estação no pedido, para os gráficos de etapa sem buscar o detalhe.
- Pedido em andamento ou interrompido vem com D, P, Q e OEE `null`: fica fora do OEE.

`GET /api/pedidos/detalhe/42`

```json
{
  "pedido": { "id": 42, "numero": 42, "situacao": "CONCLUIDO", "leadTimeS": 100.0, "oee": 80.0 },
  "etapas": [
    { "estacao": "ESTOQUE", "inicio": "2026-11-17T13:02:10.250Z", "fim": "2026-11-17T13:02:31.250Z",
      "duracaoS": 21.0, "tempoParadoS": 0.0, "tempoPadraoS": 20.0 },
    { "estacao": "MANIPULACAO", "inicio": "2026-11-17T13:02:31.250Z", "fim": "2026-11-17T13:03:18.250Z",
      "duracaoS": 47.0, "tempoParadoS": 12.0, "tempoPadraoS": 35.0 },
    { "estacao": "SEPARACAO", "inicio": "2026-11-17T13:03:18.250Z", "fim": "2026-11-17T13:03:44.250Z",
      "duracaoS": 26.0, "tempoParadoS": 0.0, "tempoPadraoS": 25.0 }
  ]
}
```

`pedido` é o mesmo objeto da listagem (resumido acima); `etapas` vêm em ordem de início.

`GET /api/indicadores/historico/1`

```json
[
  { "maquinaId": 1, "pedidoId": 42, "disponibilidade": 88.0, "performance": 90.909, "qualidade": 100.0, "oee": 80.0,
    "periodoInicio": "2026-11-17T13:02:10.250Z", "periodoFim": "2026-11-17T13:03:50.250Z",
    "calculadoEm": "2026-11-17T13:03:50.250Z" }
]
```

Um item por pedido concluído, do mais recente ao mais antigo (é a série do gráfico de tendência).

### Erros

Todo erro volta no mesmo formato:

```json
{
  "timestamp": "2026-11-17T10:00:00-03:00",
  "status": 404,
  "erro": "Recurso não encontrado",
  "mensagem": "Máquina não encontrada com id: 999",
  "caminho": "/api/dashboard/999"
}
```

| Situação | Status |
|---|---|
| Máquina ou pedido inexistente | `404` |
| Rota inexistente | `404` |
| Id ou parâmetro em formato inválido (ex.: `/api/dashboard/abc`, `origem=XYZ`); a mensagem diz o formato esperado | `400` |
| Método diferente de `GET` (ex.: `POST /api/maquinas`) | `405` |
| Erro inesperado | `500`, com mensagem fixa (o detalhe fica só no log) |

## Banco de dados

| Tabela | Quem grava | Conteúdo |
|---|---|---|
| `maquina` | seed do `schema.sql` e coleta | Cadastro da bancada (`BANCADA_SMART_01` fixa com `id = 1`), tempo padrão do pedido, status e última leitura |
| `pedido` | coleta do backend | Um registro por pedido: origem, situação, início, fim, lead time, tempo parado e reprovação |
| `etapa_pedido` | coleta do backend | Tempo de cada estação no pedido, com o tempo padrão da estação |
| `indicador_oee` | coleta do backend | D, P, Q e OEE de cada pedido concluído (um por pedido) |
| `leitura_planta` | coleta do backend | JSON bruto de cada mudança lida no `/dadosPlanta` |

`spring.jpa.hibernate.ddl-auto=validate`: o Hibernate não altera o banco, só confere na subida se as tabelas batem com as entidades. Por isso schema e entidades mudam sempre juntos.

### Atualizar o banco local

O banco é local e ainda não tem dado real, então a troca de versão é recriar tudo. No MySQL Workbench (ou no cliente `mysql`):

```sql
DROP DATABASE techmob;
```

e rode o [`infra/schema.sql`](../infra/schema.sql) de novo. O seed volta com a máquina `id = 1` e o tempo padrão de 80 s por pedido.

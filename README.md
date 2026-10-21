# TechMob 4.0 — Toyota do Brasil

> **Projeto Integrador Interdisciplinar II — Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas.**
>
> **Equipe:** Lucas · Nykolas · Otávio · Rafael · Samuel

## Objetivo

Construir uma aplicação completa de coleta, processamento, armazenamento e visualização de dados de produção de uma máquina industrial (**Bancada Smart 4.0**), calculando o indicador **OEE (Overall Equipment Effectiveness)** e disponibilizando essa informação em um dashboard web em tempo (quase) real.

## Arquitetura

```
 Bancada Smart 4.0
   └─ Node-RED da Smart ──MQTT──►  Broker MQTT (porta 1883)
                                        │  tópico techmob/smart/+/producao
                                        ▼
                      Node-RED do projeto (porta 1880)
                      valida o JSON → grava uma linha no CSV
                                        │
                                        ▼
                          data/dados_producao.csv   (dado bruto, ciclo a ciclo)
                                        │
                                        ▼
                  Python (oee_calculator.py) → calcula o OEE
                                        │  INSERT
                                        ▼
                       MySQL "techmob"  (maquina, indicador_oee, historico_producao)
                                        │  leitura (JPA)
                                        ▼
                     API Spring Boot (porta 8080) → JSON
                                        │  Axios, a cada 5s
                                        ▼
                      Dashboard React (porta 5173)
```

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Captura de dados | Bancada Smart 4.0, MQTT, Node-RED | Leitura dos sinais da máquina e roteamento das mensagens |
| Armazenamento bruto | Arquivos CSV | Histórico ciclo a ciclo dos dados de produção |
| Processamento | Python (pandas) | Leitura dos CSVs, cálculo do OEE e estatísticas descritivas |
| Persistência | MySQL 8 local | Máquinas, indicadores calculados, histórico consolidado |
| Back-end / API | Java 25, Spring Boot, Spring Data JPA | Endpoints REST (somente leitura) que servem o banco ao front-end |
| Front-end | React, Vite, Axios, Recharts | Dashboard com cards, gráficos e histórico |

## Estrutura do repositório

```
toyota-techmob/
├── backend/             # API Spring Boot          → ver backend/README.md
├── frontend/            # Dashboard React + Vite   → ver frontend/README.md
├── data/                # Python: cálculo do OEE e simulador
│   ├── oee_calculator.py          # lê o CSV do Node-RED e grava o OEE no MySQL
│   ├── simulador_oee.py           # gera dados simulados da bancada
│   ├── dados_producao_exemplo.csv
│   └── requirements.txt
├── node-red/            # captura MQTT da Smart
│   ├── flows.json                 # MQTT → valida → CSV
│   ├── settings.js
│   └── package.json
├── infra/
│   └── schema.sql                 # tabelas + seed da BANCADA_SMART_01 (id = 1)
├── start-node-red.cmd   # sobe o Node-RED em http://localhost:1880
└── README.md
```

## Pré-requisitos

- JDK 25 (o Maven vem junto, pelo wrapper `mvnw`)
- Node.js 20+ e npm
- Python 3.11+
- MySQL 8 (Server + Workbench)
- Broker MQTT na porta 1883 (ex.: Mosquitto)

## Como rodar (do zero)

Cada parte roda num terminal separado. A ordem abaixo segue o caminho do dado.

### 1. Banco de dados

Execute `infra/schema.sql` no MySQL Workbench (ou, no PowerShell, `Get-Content infra/schema.sql | mysql -u root -p`). O script cria o banco `techmob`, as tabelas e a máquina `BANCADA_SMART_01` com `id = 1`.

As credenciais vêm de variáveis de ambiente, usadas tanto pelo backend quanto pelo Python:

| Variável | Padrão |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `techmob` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(obrigatória)* |

```powershell
$env:DB_PASSWORD = "sua-senha"
```

### 2. Node-RED (captura da Smart)

```powershell
npm install --prefix node-red   # só na primeira vez
.\start-node-red.cmd
```

O editor abre em http://localhost:1880. O flow assina o tópico `techmob/smart/+/producao` no broker `127.0.0.1:1883` e grava cada mensagem em `data/dados_producao.csv`.

A Smart deve publicar um JSON neste formato:

```json
{
  "maquina": "BANCADA_SMART_01",
  "status": "EM_PRODUCAO",
  "pecas_boas": 1,
  "pecas_defeituosas": 0,
  "tempo_ciclo_segundos": 9.8,
  "timestamp": "2026-10-20T08:00:10Z"
}
```

`maquina`, `status`, `pecas_boas` e `pecas_defeituosas` são obrigatórios. Sem `timestamp`, o Node-RED usa a hora atual. Para o OEE, o tempo só conta como produção quando `status` é exatamente `EM_PRODUCAO`.

Para testar sem a bancada (com o Mosquitto instalado), salve o JSON acima em `mensagem.json` e publique:

```powershell
mosquitto_pub -t techmob/smart/01/producao -f mensagem.json
```

Em produção, defina `NODE_RED_CREDENTIAL_SECRET` no ambiente; sem ela, o Node-RED usa uma chave de desenvolvimento.

### 3. Python (cálculo do OEE)

```powershell
cd data
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt

# processa um CSV e grava o OEE no MySQL
python oee_calculator.py --csv dados_producao_exemplo.csv --maquina-id 1 --gravar-banco

# ou fica vigiando a pasta e processa CSVs novos a cada 30s
python oee_calculator.py --watch-dir . --maquina-id 1 --gravar-banco --intervalo 30
```

Sem `--gravar-banco`, o script só calcula e mostra o resultado no console, sem precisar do MySQL.

Para gerar dados de teste:

```powershell
python simulador_oee.py --simular --horas 8 --seed 42 --saida dados_simulados.csv --relatorio
```

### 4. Backend (API)

```powershell
cd backend
.\run-local.cmd          # pede a senha se DB_PASSWORD não estiver definida
```

- API: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- Testes (não precisam de MySQL): `.\mvnw.cmd test`

Endpoints, exemplos de JSON e detalhes em [`backend/README.md`](backend/README.md).

### 5. Frontend (dashboard)

```powershell
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173. Detalhes, modo simulação e situação da integração em [`frontend/README.md`](frontend/README.md).

## Portas

| Serviço | Porta |
|---|---|
| Frontend (Vite) | 5173 |
| Backend (Spring Boot) | 8080 |
| Node-RED | 1880 |
| Broker MQTT | 1883 |
| MySQL | 3306 |

## Pendências conhecidas

**Conexão com a Smart**
- O broker MQTT precisa aceitar conexões da rede. O Mosquitto, por padrão, só escuta em `127.0.0.1`, e a porta 1883 precisa estar liberada no firewall do Windows. Outra opção é apontar o flow para o broker da própria Smart.
- O tópico, os nomes dos campos e os valores de `status` precisam ser combinados com a equipe da Smart.

**Fluxo de dados**
- O Node-RED grava o CSV **sem a linha de cabeçalho**, e o `oee_calculator.py` exige as colunas `timestamp,bancada_id,status_operacional,pecas_boas,pecas_defeituosas,tempo_ciclo_segundos`.
- Ninguém grava em `historico_producao` nem atualiza o `status_operacional` da máquina.
- O `oee_calculator.py` usa 8h fixas como tempo planejado, e o simulador usa a janela real dos dados. Os dois também usam capacidades teóricas diferentes (ciclo de 10s e de 8,5s), o que pode gerar performance acima de 100%.

**Frontend**
- O Dashboard exibe sempre os dados da simulação, e Produção e Histórico ainda não chamam a API. Detalhes em [`frontend/README.md`](frontend/README.md#pendências).

## Equipe TechMob 4.0

© 2026

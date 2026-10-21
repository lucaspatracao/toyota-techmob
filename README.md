# TechMob 4.0 — Toyota do Brasil

> **Projeto Integrador Interdisciplinar II — Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas.**
> 
> **Equipe:** Lucas · Nykolas · Otávio · Rafael · Samuel

## Objetivo

Construir uma aplicação completa de coleta, processamento, armazenamento e visualização de dados de produção de uma máquina industrial (**Bancada Smart 4.0**), calculando o indicador **OEE (Overall Equipment Effectiveness)** e disponibilizando essa informação em um dashboard web em tempo (quase) real.

## Arquitetura

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Captura de dados | Bancada Smart 4.0, MQTT Broker, Node-RED | Leitura dos sinais da máquina e roteamento das mensagens |
| Armazenamento bruto | Arquivos CSV | Histórico ciclo a ciclo dos dados de produção |
| Processamento / Ciência de Dados | Python (pandas, NumPy) | Leitura dos CSVs, cálculo do OEE e estatísticas descritivas |
| Persistência estrutural | MySQL local (Workbench / localhost) | Máquinas, indicadores calculados, histórico consolidado |
| Back-end / API | Java 25, Spring Boot, Spring Data JPA | Endpoints REST que servem os dados do banco ao front-end |
| Front-end | React, Axios, Chart.js/Recharts | Dashboard interativo com cards, gráficos e histórico |
| Deploy / execução local | Java + Maven + MySQL local | Execução do back-end e banco no ambiente local |

Fluxo geral: **Bancada Smart 4.0 → MQTT → Node-RED → CSV / MySQL → API REST (Spring Boot) → Dashboard (React)**

## Estrutura do repositório

- `backend/`: API Spring Boot
- `frontend/`: aplicação React
- `infra/`: schema MySQL e seed inicial
- `data/`: dados e scripts auxiliares
- `node/`: flow do Node-RED

```
toyota-techmob/
│
├── backend/            # Spring Boot (pacote com.toyota.techmob.backend)
│   ├── src/main/java/com/toyota/techmob/backend/
│   │   ├── domain/         # Entidades JPA: Maquina, IndicadorOEE, HistoricoProducao
│   │   ├── repository/     # Repositórios Spring Data JPA
│   │   ├── controller/     # Endpoints REST
│   │   └── exception/      # GlobalExceptionHandler, MaquinaNotFoundException
│   └── src/main/resources/
│       └── application.properties
│
├── data/               # Python para cálculo de OEE a partir dos CSVs
│   ├── oee_calculator.py
│   └── requirements.txt
│
├── frontend/           # React + Vite
│   ├── src/
│   │   ├── components/ # Componentes reutilizáveis
│   │   ├── data/       # Dados mockados temporariamente
│   │   ├── pages/      # Dashboard, Produção e Histórico
│   │   └── styles/     # Estilos da aplicação
│
├── infra/
│   └── schema.sql          # schema do MySQL local
│
├── node/
│   ├── flow-captura-bancada.json
│   └── README.md
│
└── README.md
```

- Java 25+
- Maven
- Node.js + npm
- MySQL local

### Pré-requisitos
- JDK 25 e Maven
- Node.js e npm
- MySQL Workbench / MySQL Server local

Crie o banco e execute o script em `infra/schema.sql`.

```sql
CREATE DATABASE IF NOT EXISTS techmob CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE techmob;
```

Importe o conteúdo de `infra/schema.sql` no MySQL Workbench ou via cliente MySQL.

## Back-end

A senha do banco não fica hardcoded no projeto. Para rodar localmente, defina as variáveis de ambiente antes de iniciar a aplicação:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "123456"
$env:DB_URL = "jdbc:mysql://localhost:3306/techmob?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"

cd "c:\Users\49615129828\toyota-techmob\backend"
.\mvnw.cmd spring-boot:run
```

Também há um atalho para uso rápido no Windows:

```powershell
cd "c:\Users\49615129828\toyota-techmob\backend"
.\run-local.cmd
```

O back-end sobe na porta padrão do Spring Boot (`8080`) e se conecta ao MySQL local usando as variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD`.

No Windows PowerShell, configure ao menos a senha antes de iniciar:

```powershell
$env:DB_PASSWORD = "sua-senha"
```

- http://localhost:8080
- health check: http://localhost:8080/actuator/health

## Front-end

```powershell
cd "c:\Users\49615129828\toyota-techmob\frontend"
Set-ExecutionPolicy -Scope Process Bypass
npm install
npm run dev -- --host 0.0.0.0
```

A aplicação local geralmente é acessada em:

- http://localhost:5173

```bash
cd data
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python oee_calculator.py --csv dados_producao_exemplo.csv --maquina-id 1 --gravar-banco
```

- O seed em `infra/schema.sql` fixa a máquina `BANCADA_SMART_01` com `id = 1` para manter a compatibilidade das rotas e do dashboard.
- Os endpoints do backend foram organizados para leitura direta do MySQL com transações somente leitura.
- O CORS está configurado para aceitar requisições do frontend local (`localhost:5173`).

## Equipe TechMob 4.0

© 2026
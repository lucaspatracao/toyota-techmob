# TechMob 4.0 — Toyota do Brasil

> **Projeto Integrador Interdisciplinar II — Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas.**
> 
> **Equipe:** Lucas · Nykolas · Otávio · Rafael · Samuel

---

## Objetivo

Construir uma aplicação completa de coleta, processamento, armazenamento e visualização de dados de produção de uma máquina industrial (**Bancada Smart 4.0**), calculando o indicador **OEE (Overall Equipment Effectiveness)** e disponibilizando essa informação em um dashboard web em tempo (quase) real, hospedado em nuvem.

## Arquitetura

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Captura de dados | Bancada Smart 4.0, MQTT Broker, Node-RED | Leitura dos sinais da máquina e roteamento das mensagens |
| Armazenamento bruto | Arquivos CSV | Histórico ciclo a ciclo dos dados de produção |
| Processamento / Ciência de Dados | Python (pandas, NumPy) | Leitura dos CSVs, cálculo do OEE e estatísticas descritivas |
| Persistência estrutural | MySQL local (Workbench / localhost) | Máquinas, indicadores calculados, histórico consolidado |
| Back-end / API | Java 17, Spring Boot, Spring Data JPA | Endpoints REST que servem os dados do banco ao front-end |
| Front-end | React, Axios, Chart.js/Recharts | Dashboard interativo com cards, gráficos e histórico |
| Deploy / execução local | Java + Maven + MySQL local | Execução do back-end e banco no ambiente local |

Fluxo geral: **Bancada Smart 4.0 → MQTT → Node-RED → CSV / MySQL → API REST (Spring Boot) → Dashboard (React)**

> Decisão de arquitetura: a persistência é híbrida — dados brutos ciclo a ciclo em CSV, dados estruturais e indicadores calculados no MySQL local.

## Estrutura do Repositório

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
├── data-science/       # Python para cálculo de OEE a partir dos CSVs
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
├── node-red/
│   ├── flow-captura-bancada.json
│   └── README.md
│
└── README.md
```

## Como rodar localmente

### Pré-requisitos
- Java 17+ e Maven
- Node.js e npm
- MySQL Workbench / MySQL Server local

### Banco local MySQL

Crie o banco e use o schema em `infra/schema.sql`.

```sql
CREATE DATABASE IF NOT EXISTS techmob CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE techmob;
```

Importe o conteúdo de `infra/schema.sql` no MySQL Workbench, ou execute diretamente o script no cliente MySQL.

### Back-end

```bash
cd backend
./mvnw spring-boot:run
```

No Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

O back-end sobe na porta padrão do Spring Boot (`8080`) e se conecta ao MySQL local usando as configurações em `backend/src/main/resources/application.properties`.

### Front-end

```bash
cd frontend
npm install
npm run dev
```

O front-end atualmente utiliza React + Vite e dados mockados como fallback durante a integração com a API. A aplicação estará disponível em `http://localhost:5173` por padrão.

### Python / cálculo do OEE

```bash
cd data-science
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python oee_calculator.py --csv dados_producao_exemplo.csv --maquina-id 1 --gravar-banco
```

Esse script lê o CSV bruto e grava os indicadores no banco MySQL local usando as variáveis de ambiente `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD`.

---

**Equipe TechMob 4.0 © 2026**
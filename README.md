# TechMob 4.0 — Toytota do Brasil

> **Projeto Integrador Interdisciplinar II — Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas.**
> 
> **Equipe:** Lucas · Nykolas · Otávio · Rafael · Samuel

---

## 🎯 Objetivo

Construir uma aplicação completa de coleta, processamento, armazenamento e visualização de dados de produção de uma máquina industrial (**Bancada Smart 4.0**), calculando o indicador **OEE (Overall Equipment Effectiveness)** e disponibilizando essa informação em um dashboard web em tempo (quase) real, hospedado em nuvem.

## 🏗️ Arquitetura

| Camada | Tecnologia | Responsabilidade |
|---|---|---|
| Captura de dados | Bancada Smart 4.0, MQTT Broker, Node-RED | Leitura dos sinais da máquina e roteamento das mensagens |
| Armazenamento bruto | Arquivos CSV | Histórico ciclo a ciclo dos dados de produção |
| Processamento / Ciência de Dados | Python (pandas, NumPy) | Leitura dos CSVs, cálculo do OEE e estatísticas descritivas |
| Persistência estrutural | PostgreSQL (Supabase) | Máquinas, indicadores calculados, histórico consolidado |
| Back-end / API | Java 17, Spring Boot, Spring Data JPA | Endpoints REST que servem os dados do banco ao front-end |
| Front-end | React, Axios, Chart.js/Recharts | Dashboard interativo com cards, gráficos e histórico |
| Nuvem / Deploy | Render, Vercel, Supabase | Hospedagem do back-end, front-end e banco de dados |

Fluxo geral: **Bancada Smart 4.0 → MQTT → Node-RED → CSV / PostgreSQL → API REST (Spring Boot) → Dashboard (React)**

> Decisão de arquitetura: a persistência é **híbrida** — dados brutos ciclo a ciclo em CSV, dados estruturais e indicadores calculados no PostgreSQL (Supabase).

## 📁 Estrutura do Repositório

```
toyota-techmob/
│
├── backend/            # Spring Boot (pacote com.toyota.techmob.backend)
│   ├── src/main/java/com/toyota/techmob/backend/
│   │   ├── model/          # Entidades JPA: Maquina, IndicadorOEE, HistoricoProducao
│   │   ├── repository/     # Repositórios Spring Data JPA
│   │   ├── controller/     # Endpoints REST
│   │   └── exception/      # GlobalExceptionHandler, MaquinaNotFoundException
│   └── src/main/resources/
│       └── application.properties
│
├── front-end/          # React (Create React App)
│   └── src/
│       ├── services/       # api.js, maquinaService.js (Axios)
│       └── pages/          # Dashboard.jsx
│
└── infra/
    ├── docker-compose.yml  # não utilizado atualmente (ver decisões abaixo)
    └── schema.sql          # fonte da verdade do schema (já aplicado no Supabase)
```

## 🚀 Como rodar localmente

### Pré-requisitos
- Java 17+ e Maven
- Node.js e npm
- Acesso ao projeto Supabase do time (host, schema e `DB_PASSWORD`)

### Back-end

```bash
cd backend
export DB_PASSWORD=<senha do Supabase>
./mvnw spring-boot:run
```

O back-end sobe na porta padrão do Spring Boot (`8080`) e se conecta ao Supabase usando `application.properties`.

### Front-end

```bash
cd front-end
npm install
npm start
```

O front-end sobe em `localhost:3000` e consome a API via `services/api.js`.

---

**Equipe TechMob 4.0 © 2026**

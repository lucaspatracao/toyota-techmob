# TechMob 4.0 — Toyota do Brasil

> **Projeto Integrador Interdisciplinar II — Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas.**
> 
> **Equipe:** Lucas · Nykolas · Otávio · Rafael · Samuel

## Objetivo

Construir uma aplicação completa de coleta, processamento, armazenamento e visualização de dados de produção de uma máquina industrial (**Bancada Smart 4.0**), calculando o indicador **OEE (Overall Equipment Effectiveness)** e disponibilizando essa informação em um dashboard web em tempo (quase) real.

## Arquitetura

- Captura de dados: Bancada Smart 4.0, MQTT Broker, Node-RED
- Armazenamento bruto: CSV
- Processamento / Ciência de Dados: Python
- Persistência estrutural: MySQL local
- Back-end / API: Java 25 + Spring Boot + Spring Data JPA
- Front-end: React + Vite

## Estrutura do repositório

- `backend/`: API Spring Boot
- `frontend/`: aplicação React
- `infra/`: schema MySQL e seed inicial
- `data/`: dados e scripts auxiliares
- `node/`: flow do Node-RED

## Requisitos locais

- Java 25+
- Maven
- Node.js + npm
- MySQL local

## Banco de dados local

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

A API fica disponível em:

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

## Observações importantes

- O seed em `infra/schema.sql` fixa a máquina `BANCADA_SMART_01` com `id = 1` para manter a compatibilidade das rotas e do dashboard.
- Os endpoints do backend foram organizados para leitura direta do MySQL com transações somente leitura.
- O CORS está configurado para aceitar requisições do frontend local (`localhost:5173`).

## Equipe TechMob 4.0

© 2026
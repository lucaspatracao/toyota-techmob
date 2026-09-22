# Backend TechMob

Este diretório contém a API REST em Java com Spring Boot responsável por expor os dados do MySQL para o frontend.

## Requisitos

- Java 25+
- Maven
- MySQL local

## Variáveis de ambiente

As credenciais e a URL do banco devem ser configuradas via ambiente, não no arquivo de configuração do projeto.

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "123456"
$env:DB_URL = "jdbc:mysql://localhost:3306/techmob?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"
```

## Execução local

### Opção 1: comando direto

```powershell
cd "c:\Users\49615129828\toyota-techmob\backend"
.\mvnw.cmd spring-boot:run
```

### Opção 2: atalho do projeto

```powershell
cd "c:\Users\49615129828\toyota-techmob\backend"
.\run-local.cmd
```

## Endpoints principais

- `GET /api/maquinas`
- `GET /api/dashboard/{maquinaId}`
- `GET /api/historico-producao/{maquinaId}`
- `GET /api/indicadores/historico/{maquinaId}`
- `GET /actuator/health`

## Observações

- O banco de dados local deve conter o schema em `infra/schema.sql`.
- A máquina `BANCADA_SMART_01` é mantida com `id = 1` no seed para preservar compatibilidade das chamadas da API.
- As consultas de leitura são executadas como transações somente leitura para manter a integração com o MySQL mais estável.

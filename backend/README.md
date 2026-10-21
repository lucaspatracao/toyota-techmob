# Backend TechMob

Este diretório contém a API REST em Java com Spring Boot responsável por expor os dados do MySQL para o frontend.

## Requisitos

- Java 25+
- Maven
- MySQL local

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

```powershell
$env:DB_PASSWORD = "sua-senha"
```

## Execução local

### Opção 1: comando direto

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### Opção 2: atalho do projeto (pede a senha se `DB_PASSWORD` não estiver definida)

```powershell
cd backend
.\run-local.cmd
```

## Testes

```powershell
.\mvnw.cmd test
```

Não precisam de MySQL. Os testes de service usam mocks, e o `ApiContratoIntegrationTest` sobe a API com um banco H2 em memória e confere o JSON de cada endpoint.

## Estrutura

```
controller/  recebe as requisições HTTP
service/     regras de negócio
repository/  acesso ao banco (Spring Data JPA)
domain/      entidades JPA (espelham as tabelas)
dto/         respostas da API (records)
mapper/      conversão entidade → DTO
exception/   tratamento global de erros
config/      CORS
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

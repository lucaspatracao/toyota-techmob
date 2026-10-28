# Backend TechMob 4.0

API REST em Java (Spring Boot) que lê o MySQL e entrega os dados de produção e OEE da Bancada Smart 4.0 para o dashboard.

A API é **somente leitura**: quem grava no banco é o módulo Python ([`data/oee_calculator.py`](../data/oee_calculator.py)), a partir do CSV que o Node-RED gera com os dados da Smart.

```
Smart → MQTT → Node-RED → CSV → Python (OEE) → MySQL → API (este projeto) → Dashboard
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

São as mesmas variáveis que o `oee_calculator.py` usa, então basta configurar uma vez.

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

## Testes

```powershell
.\mvnw.cmd test
```

Não precisam de MySQL:

- **Testes de service** (`service/*Test`): usam mocks dos repositórios.
- **`ApiContratoIntegrationTest`**: sobe a API inteira com um banco H2 em memória, chama cada endpoint por HTTP e confere o JSON exato. Se algum campo mudar de nome ou formato, o teste falha antes de quebrar o frontend.

## Estrutura

```
src/main/java/com/toyota/techmob/backend/
├── controller/  recebe as requisições HTTP e devolve JSON
├── service/     regras de negócio (interface + implementação)
├── repository/  acesso ao banco (Spring Data JPA; a consulta sai do nome do método)
├── domain/      entidades JPA, espelham as tabelas
├── dto/         respostas da API (records); a entidade nunca é exposta direto
├── mapper/      conversão entidade → DTO, num lugar só
├── exception/   tratamento global de erros
└── config/      CORS (libera o frontend em localhost:5173 e :3000)
```

Caminho de uma requisição: `Controller → Service → Repository → MySQL`, e na volta `Entidade → DtoMapper → DTO → JSON`.

## Endpoints

Todos são `GET`.

| Rota | Retorna |
|---|---|
| `/api/maquinas` | Lista de máquinas |
| `/api/dashboard/{maquinaId}` | Máquina + OEE mais recente, numa chamada só |
| `/api/indicadores/historico/{maquinaId}` | Série de OEE, do mais recente ao mais antigo |
| `/api/historico-producao/{maquinaId}` | Ciclos de produção, do mais recente ao mais antigo |
| `/actuator/health` | Situação da API e do banco |

### Exemplos de resposta

`GET /api/dashboard/1`

```json
{
  "maquina": { "id": 1, "bancadaId": "BANCADA_SMART_01", "nome": "Bancada Smart 4.0", "statusAtual": "EM_PRODUCAO" },
  "indicadorAtual": {
    "maquinaId": 1, "disponibilidade": 90.0, "performance": 85.0,
    "qualidade": 97.5, "oee": 74.6, "calculadoEm": "2026-08-17T10:00:00Z"
  }
}
```

`indicadorAtual` vem `null` quando o Python ainda não calculou nenhum OEE para a máquina.

`GET /api/historico-producao/1`

```json
[
  { "maquinaId": 1, "timestamp": "2026-08-17T08:00:10Z", "statusOperacional": "EM_PRODUCAO",
    "pecasBoas": 1, "pecasDefeituosas": 0, "tempoCicloSegundos": 9.800 }
]
```

### Erros

Máquina inexistente devolve `404`, e qualquer outro erro devolve `500`, sempre no mesmo formato:

```json
{
  "timestamp": "2026-10-20T22:00:00-03:00",
  "status": 404,
  "erro": "Recurso não encontrado",
  "mensagem": "Máquina não encontrada com id: 999",
  "caminho": "/api/dashboard/999"
}
```

## Banco de dados

| Tabela | Quem grava | Conteúdo |
|---|---|---|
| `maquina` | seed do `schema.sql` | Cadastro da bancada (`BANCADA_SMART_01` fixa com `id = 1`) |
| `indicador_oee` | `oee_calculator.py` | Disponibilidade, performance, qualidade e OEE por período |
| `historico_producao` | *ainda ninguém* | Ciclos de produção consolidados |

`spring.jpa.hibernate.ddl-auto=validate`: o Hibernate não altera o banco, só confere na subida se as tabelas batem com as entidades.

## Pendências para a integração com a Smart

- **`historico_producao` fica vazia**: nenhum componente grava nela, então `/api/historico-producao` volta `[]`. Caminhos possíveis: o Node-RED grava direto no MySQL, ou a API ganha um `POST` para o Node-RED chamar.
- **`maquina.status_operacional` nunca é atualizado** depois do seed, pelo mesmo motivo.
- Para o `POST`, o projeto já tem `spring-boot-starter-validation` (para `@Valid`) e `MaquinaRepository.findByIdentificadorBancada` (para achar a máquina pelo `bancada_id` que vem da Smart).

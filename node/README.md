# Flow Node-RED — Captura MQTT da Bancada Smart 4.0

## O que este flow faz

1. **MQTT In** — inscreve nos tópicos `producao/pecas` e `maquina/status` publicados pela Bancada Smart 4.0.
2. **Function (parse/validação)** — normaliza o payload recebido para o formato de linha do CSV definido no Documento do Projeto (`timestamp,bancada_id,status_operacional,pecas_boas,pecas_defeituosas,tempo_ciclo_segundos`), descartando mensagens com status operacional inválido.
3. **File (append)** — grava cada leitura como uma nova linha no CSV de produção (`/data/csv/dados_producao.csv`), que depois é consumido pelo módulo Python.
4. **MySQL (opcional)** — atualiza o status atual da máquina diretamente na tabela `maquina` do banco local, para refletir o estado em tempo real sem esperar o processamento em lote do Python.

Requer os nós MQTT nativos do Node-RED e, se a integração com o banco estiver ativa, o nó de conexão MySQL (`node-red-node-mysql` ou equivalente).

## Como importar

1. Abra o Node-RED → menu (☰) → **Import** → cole o conteúdo de `flow-captura-bancada.json` ou selecione o arquivo.
2. Antes de fazer o deploy, ajuste:
   - **Nó `mqtt-broker-bancada`**: host/porta reais do broker MQTT da bancada (hoje configurado como `localhost:1883` — placeholder).
   - **Nó `file-append-csv`**: caminho do arquivo CSV (`/data/csv/dados_producao.csv`). Se o módulo Python roda na mesma máquina, aponte para o mesmo diretório que o `--csv` do `oee_calculator.py` vai ler.
   - **Nó `mysql-config-local`**: host, porta, usuário, senha e nome do banco local (`techmob`). Use as credenciais do MySQL Workbench local, por exemplo `root` / `123456`.
3. Confirme os nomes dos tópicos MQTT (`producao/pecas`, `maquina/status`) contra a documentação real da bancada — os nomes usados aqui são os exemplos citados no Documento do Projeto (seção 5.1).
4. Deploy.

## Formato de payload MQTT esperado

O nó de função espera um payload JSON como:

```json
{
  "timestamp": "2026-08-17T14:32:10Z",
  "bancada_id": "BANCADA_SMART_01",
  "status_operacional": "EM_PRODUCAO",
  "pecas_boas": 1,
  "pecas_defeituosas": 0,
  "tempo_ciclo_segundos": 12.5
}
```

Se a bancada publicar em formato diferente, ajuste o nó `function-parse-validacao` para o mapeamento correto dos campos.

## Próximo passo

Depois de validar que o CSV está sendo gravado corretamente, use `data-science/oee_calculator.py` para calcular os indicadores a partir dele e gravar os dados em MySQL local.

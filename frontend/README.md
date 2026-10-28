# SMART 4.0 — Frontend (TechMob 4.0)

Dashboard web (Vite + React) de monitoramento de eficiência fabril (OEE) da Bancada Smart 4.0, do projeto TechMob 4.0 (Fundação Toyota do Brasil / SENAI-SP).

Três telas: **Dashboard**, **Produção** e **Histórico**.

## Como rodar

```powershell
cd frontend
npm install
copy .env.example .env   # ajuste VITE_API_URL se a API não estiver em localhost:8080
npm run dev
```

Abra http://localhost:5173.

| Variável (`.env`) | Padrão | Para quê |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | Endereço da API Spring Boot |
| `VITE_MAQUINA_ID` | `1` | Máquina consultada (hoje só existe a Bancada Smart 4.0) |

### Build de produção

```powershell
npm run build     # gera dist/
npm run preview   # serve o build em http://localhost:4173
```

A pasta `dist/` é gerada a cada build e **não é versionada**. O build separa as bibliotecas em arquivos próprios (`react-*.js`, `charts-*.js`), que o navegador mantém em cache entre deploys. Assim, uma mudança no código da aplicação só faz baixar de novo o arquivo `index-*.js`, de cerca de 90 kB.

## Modo simulação

O botão **"Desligar/Ativar simulação"** na barra superior controla um gerador de dados fictícios (`src/data/simulation.js`), que atualiza as telas a cada 4 segundos. Serve para apresentar o dashboard sem a bancada ligada.

## Integração com a API

A camada de acesso fica em `src/services/`:

| Arquivo | Papel |
|---|---|
| `api.js` | Instância do Axios (`baseURL` vem de `VITE_API_URL`, timeout de 10s) |
| `machineService.js` | Uma função por endpoint da API |
| `adapters.js` | "De-para" do JSON da API para o formato das telas (o JSON de cada endpoint está documentado no topo do arquivo) |

O contrato completo da API, com exemplos de resposta, está em [`backend/README.md`](../backend/README.md).

### Situação atual

| Tela | Fonte dos dados |
|---|---|
| Dashboard | Chama `GET /api/dashboard/{id}` a cada 5s (`usePolling`), **mas exibe os dados da simulação** (ver pendências) |
| Produção | Simulação / `mockData.js` |
| Histórico | Simulação / `mockData.js` |

### Pendências

1. **O Dashboard mostra sempre a simulação.** Em `Dashboard.jsx` a ordem é `simulationState ?? data`, e o `App` sempre entrega um estado de simulação, mesmo com ela desligada. Para exibir a API, a simulação desligada precisa enviar `simulationState = null`.
2. **Os adapters esperam campos que a API não envia.** Só os 4 indicadores de `indicadorAtual` (oee, disponibilidade, performance, qualidade) batem. A série de OEE, o resumo de 24h e as funções de histórico e máquinas precisam ser ajustados ao JSON real.
3. **Produção e Histórico** ainda não chamam a API. As funções `buscarHistoricoProducao` e `buscarHistoricoIndicadoresOEE` já existem em `machineService.js` e podem seguir o padrão do Dashboard.

## Estrutura de pastas

```
frontend/
├─ index.html                 # página base (favicon: /logo-smart.svg)
├─ vite.config.js             # porta 5173 + divisão do bundle
├─ public/
│  └─ logo-smart.svg
└─ src/
   ├─ main.jsx                # bootstrap do React
   ├─ App.jsx                 # layout (sidebar + topbar), rotas, tema claro/escuro e simulação
   ├─ components/
   │  ├─ Sidebar.jsx          # menu lateral (logo, navegação, status da máquina)
   │  ├─ Topbar.jsx           # barra superior (tema, simulação, status MQTT)
   │  ├─ PageHeader.jsx       # cabeçalho de cada página (título, status, relógio)
   │  ├─ Panel.jsx            # card genérico usado por gráficos e tabelas
   │  ├─ StatCard.jsx         # card de indicador numérico
   │  ├─ SummaryDonut.jsx     # donut de peças boas x rejeitadas
   │  ├─ StateFeedback.jsx    # estados de carregando / erro / vazio
   │  └─ Pagination.jsx       # paginação da tabela de Histórico
   ├─ pages/
   │  ├─ Dashboard.jsx
   │  ├─ Producao.jsx
   │  └─ Historico.jsx
   ├─ services/               # api.js, machineService.js, adapters.js
   ├─ hooks/
   │  └─ usePolling.js        # chama uma função a cada N ms, com loading/erro
   ├─ data/
   │  ├─ mockData.js          # dados fictícios fixos
   │  └─ simulation.js        # gerador do modo simulação
   └─ styles/                 # um CSS por componente/página + global.css (tokens de cor)
```

## Decisões de interface

- O ícone da bancada na sidebar foi recriado em SVG, porque o original não estava disponível isolado.
- O relógio do cabeçalho mostra a hora atual do navegador, para reforçar o "tempo real".
- A paginação (Histórico) e o seletor de intervalo (Produção) já funcionam sobre os dados de exemplo.

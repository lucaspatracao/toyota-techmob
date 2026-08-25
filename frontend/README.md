# SMART 4.0 — Frontend (TechMob 4.0)

Interface web (Vite + React) para o dashboard de monitoramento de eficiência
fabril (OEE) do projeto TechMob 4.0 — Fundação Toyota do Brasil / SENAI-SP.

Reproduz fielmente as 3 telas de referência: **Dashboard**, **Produção** e
**Histórico**.

## Como rodar

```bash
npm install
npm run dev
```

Abra `http://localhost:5173`. Build de produção: `npm run build` (gera a
pasta `dist/`).

## Estrutura de pastas

```
techmob-frontend/
├─ index.html
├─ package.json
├─ vite.config.js
└─ src/
   ├─ main.jsx                 # bootstrap do React
   ├─ App.jsx                  # layout raiz + rotas (react-router)
   ├─ components/
   │  ├─ Sidebar.jsx           # menu lateral (logo, nav, status da máquina)
   │  ├─ PageHeader.jsx        # cabeçalho de cada página (título + status online + data/hora)
   │  ├─ Panel.jsx             # card/painel genérico (usado por todos os gráficos e tabelas)
   │  ├─ StatCard.jsx          # card de indicador numérico (Produção/Histórico)
   │  ├─ ArcGauge.jsx          # anel de progresso (card OEE do Dashboard)
   │  ├─ ProgressBar.jsx       # barra linear (Disponibilidade/Performance/Qualidade)
   │  ├─ SummaryDonut.jsx      # donut peças boas x rejeitadas (Resumo 24h)
   │  └─ Pagination.jsx        # paginação da tabela de Histórico
   ├─ pages/
   │  ├─ Dashboard.jsx         # tela "Dashboard"
   │  ├─ Producao.jsx          # tela "Produção"
   │  └─ Historico.jsx         # tela "Histórico"
   ├─ data/
   │  └─ mockData.js           # dados fictícios (substituir pela API REST /api/...)
   └─ styles/
      ├─ global.css            # tokens de cor/tipografia + reset
      ├─ layout.css, sidebar.css, pageheader.css, panel.css,
      │  statcard.css, progressbar.css, pagination.css,
      │  dashboard.css, producao.css, historico.css
```

## Integração com o back-end (Spring Boot)

Os componentes hoje consomem `src/data/mockData.js`. Para plugar na API REST
descrita no Documento do Projeto (`GET /api/dashboard/{maquinaId}`,
`GET /api/indicadores/historico/{maquinaId}`, etc.), basta:

1. Criar um client HTTP (ex.: `src/services/api.js` com `axios` ou `fetch`).
2. Substituir os arrays importados de `mockData.js` por `useEffect` + `useState`
   que chamam o client e populam os componentes (`StatCard`, `ArcGauge`,
   gráficos `recharts`, tabelas).
3. Repetir a cada 5s (`setInterval`) para simular a atualização "tempo
   (quase) real" mencionada no documento (dados via MQTT → Node-RED → CSV/SGBD → API).

## Suposições assumidas (não estavam 100% claras nas imagens)

- **Ícone da bancada** na sidebar foi recriado em SVG estilizado (o asset
  original não estava disponível isolado nas capturas).
- **Relógio do cabeçalho** (`PageHeader`) foi implementado dinâmico
  (hora atual do navegador) em vez de fixo, para refletir "tempo real".
- Botões de **paginação** (Histórico) e **seletor de intervalo** (Produção)
  são funcionais sobre os dados mock, prontos para receber dados reais da API.
- O tooltip "ⓘ" ao lado de "Produção ao longo do tempo" foi mantido como
  texto estático (sem popover), já que seu conteúdo não aparece na referência.

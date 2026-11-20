import { useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import { ErrorState, LoadingState } from '../components/StateFeedback.jsx'
import { usePolling } from '../hooks/usePolling.js'
import {
  MAQUINA_ID, buscarDashboard, buscarHistoricoIndicadoresOEE, buscarPedido,
} from '../services/machineService.js'
import {
  adaptDashboard, adaptIndicadores, adaptPedidoDetalhe, corDoStatus,
  formatarDuracao, formatarHora, formatarPercentual,
} from '../services/adapters.js'
import '../styles/dashboard.css'

/** Opções do gráfico de tendência: quantos pedidos mostrar. */
const JANELAS = [10, 20, 50]

/** As setas dos KPIs comparam o último pedido com a média destes pedidos anteriores. */
const PEDIDOS_DE_REFERENCIA = 10

/** Um minuto: o detalhe de um pedido concluído não muda mais. */
const INTERVALO_PEDIDO_CONCLUIDO_MS = 60000

function KpiCard({ label, value, trend, caption, featured, progress, refreshKey, alerta }) {
  return (
    <div key={refreshKey} className={`panel kpi-card${featured ? ' kpi-card-featured' : ''}`}>
      <div className="kpi-card-top">
        <span className="kpi-label">{label}</span>
        {trend && (
          <span className={`kpi-trend ${trend.direction}`} title={trend.title}>
            {trend.direction === 'up' ? '↑' : '↓'} {trend.value}
          </span>
        )}
      </div>
      <div className="kpi-value">{value}</div>
      {progress !== undefined && (
        <div className="kpi-progress-track">
          <div className="kpi-progress-fill" style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} />
        </div>
      )}
      <div className={`kpi-caption${alerta ? ' kpi-caption-alerta' : ''}`}>{caption}</div>
    </div>
  )
}

/** OEE pedido a pedido (0 a 100), com a meta tracejada. */
function TrendChart({ points, meta }) {
  const width = 640
  const height = 190
  const y = (valor) => height - (valor / 100) * height
  const step = points.length > 1 ? width / (points.length - 1) : width
  const coords = points.map((value, index) => [index * step, y(value)])
  const linePath = coords.reduce((path, point, index) => {
    if (!index) return `M ${point[0].toFixed(1)} ${point[1].toFixed(1)}`
    const previous = coords[index - 1]
    const middleX = (previous[0] + point[0]) / 2
    return `${path} Q ${previous[0].toFixed(1)} ${previous[1].toFixed(1)} ${middleX.toFixed(1)} ${(previous[1] + point[1]) / 2} T ${point[0].toFixed(1)} ${point[1].toFixed(1)}`
  }, '')
  const areaPath = `${linePath} L ${coords.at(-1)[0]} ${height} L 0 ${height} Z`

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="dash-trend-svg" preserveAspectRatio="none" role="img"
      aria-label={`OEE dos últimos ${points.length} pedidos`}>
      <defs>
        <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--content-accent)" stopOpacity="0.35" />
          <stop offset="100%" stopColor="var(--content-accent)" stopOpacity="0" />
        </linearGradient>
      </defs>
      {meta != null && (
        <line x1="0" x2={width} y1={y(meta)} y2={y(meta)} stroke="var(--text-tertiary)" strokeWidth="1.5"
          strokeDasharray="6 5" vectorEffect="non-scaling-stroke" />
      )}
      <path d={areaPath} fill="url(#trendFill)" />
      <path d={linePath} fill="none" stroke="var(--content-accent)" strokeWidth="2.5" strokeLinecap="round" />
      {coords.map(([x, cy], index) => (
        <circle key={`${x}-${index}`} cx={x} cy={cy} r={index === coords.length - 1 ? 4 : 2.5}
          fill={meta != null && points[index] < meta ? 'var(--accent-red)' : 'var(--content-accent)'}
          stroke="var(--bg-card)" strokeWidth="2" />
      ))}
    </svg>
  )
}

/** Diferença entre o último pedido e a média dos anteriores, em pontos percentuais. */
function tendencia(atual, anteriores, campo) {
  if (atual == null || !anteriores.length) return null
  const media = anteriores.reduce((soma, i) => soma + i[campo], 0) / anteriores.length
  const delta = atual - media
  return {
    direction: delta >= 0 ? 'up' : 'down',
    value: `${Math.abs(delta).toFixed(1).replace('.', ',')} p.p.`,
    title: `Média dos ${anteriores.length} pedidos anteriores: ${formatarPercentual(media)}`,
  }
}

/** Estação que mais passou do tempo padrão no pedido (a etapa gargalo), ou null. */
function etapaMaisAtrasada(etapas) {
  return (etapas ?? [])
    .map((e) => ({ ...e, atrasoS: e.duracaoS - e.tempoPadraoS }))
    .filter((e) => e.atrasoS > 0.5)
    .sort((a, b) => b.atrasoS - a.atrasoS)[0] ?? null
}

const CLASSE_PILL = { PRODUZINDO: '', AGUARDANDO: ' status-aguardando', PARADO: ' status-parado', SEM_CONEXAO: ' status-paused' }

function BannerAviso({ titulo, texto, onReconectar }) {
  return (
    <div className="api-fallback-banner" role="status">
      <svg className="api-fallback-banner-icon" width="20" height="20" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2">
        <path d="M10.29 3.86 1.82 18a1 1 0 0 0 .86 1.5h18.64a1 1 0 0 0 .86-1.5L13.71 3.86a1 1 0 0 0-1.72 0Z" />
        <path d="M12 9v4M12 17h.01" />
      </svg>
      <div className="api-fallback-banner-text">
        <strong>{titulo}</strong>
        <span>{texto}</span>
      </div>
      {onReconectar && (
        <button className="api-fallback-reconnect" onClick={onReconectar}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 12a9 9 0 1 1-3-6.7" />
            <path d="M21 3v6h-6" />
          </svg>
          Reconectar
        </button>
      )}
    </div>
  )
}

/** Aviso sobre a origem do dado: API fora do ar, bancada sem conexão, replay ou simulação. */
function avisoDaFonte(planta, apiErro, atualizadoEm, recarregar) {
  if (apiErro) {
    return {
      titulo: 'API fora do ar: exibindo os últimos dados recebidos',
      texto: `Última resposta às ${formatarHora(atualizadoEm)}. Nova tentativa a cada poucos segundos.`,
      onReconectar: recarregar,
    }
  }
  if (!planta) return null
  if (!planta.conectado) {
    return {
      titulo: 'Bancada sem conexão',
      texto: `Nem a bancada nem o simulador responderam${planta.ultimaLeituraEm ? ` desde ${formatarHora(planta.ultimaLeituraEm)}` : ''}. Os indicadores são do último pedido gravado.`,
    }
  }
  if (planta.fonte === 'REPLAY') {
    return {
      titulo: 'Modo de demonstração: exibindo replay',
      texto: 'A bancada não está sendo lida; os pedidos vêm de uma execução real gravada no laboratório.',
    }
  }
  if (planta.fonte === 'SINTETICO') {
    return {
      titulo: 'Modo de demonstração: exibindo simulação',
      texto: 'A bancada não está sendo lida; os pedidos são gerados a partir da cronometragem da equipe.',
    }
  }
  return null
}

export default function Dashboard() {
  const [janela, setJanela] = useState(JANELAS[0])

  const dash = usePolling(() => buscarDashboard(MAQUINA_ID).then(adaptDashboard), [MAQUINA_ID])
  const indicador = dash.data?.indicadorAtual ?? null
  const pedidoId = indicador?.pedidoId ?? null

  // Série e detalhe do último pedido só mudam quando um pedido termina:
  // recarregam na hora em que o pedidoId muda e, fora isso, a cada minuto.
  const serie = usePolling(
    () => buscarHistoricoIndicadoresOEE(MAQUINA_ID).then(adaptIndicadores),
    [MAQUINA_ID, pedidoId],
    INTERVALO_PEDIDO_CONCLUIDO_MS
  )
  const ultimo = usePolling(
    () => (pedidoId ? buscarPedido(pedidoId).then(adaptPedidoDetalhe) : Promise.resolve(null)),
    [pedidoId],
    INTERVALO_PEDIDO_CONCLUIDO_MS
  )

  if (dash.loading && !dash.data) return <LoadingState label="Carregando dashboard..." />
  if (!dash.data) {
    return (
      <ErrorState
        message="Não foi possível conectar à API. Confira se o backend está rodando (porta 8080)."
        onRetry={dash.recarregar}
      />
    )
  }

  const { maquina, planta, resumoHoje, metaOee } = dash.data
  const indicadores = serie.data ?? []
  const anteriores = indicadores.filter((i) => i.pedidoId !== pedidoId).slice(0, PEDIDOS_DE_REFERENCIA)
  const pontos = indicadores.slice(0, janela).reverse().map((i) => i.oee)

  const pedido = ultimo.data?.pedido?.id === pedidoId ? ultimo.data.pedido : null
  const gargalo = ultimo.data?.pedido?.id === pedidoId ? etapaMaisAtrasada(ultimo.data.etapas) : null
  const abaixoDaMeta = indicador && metaOee != null && indicador.oee < metaOee
  const aviso = avisoDaFonte(planta, dash.error, dash.atualizadoEm, dash.recarregar)

  const emAndamento = planta?.pedidoEmAndamento ?? null
  const acimaDoPadrao = emAndamento && emAndamento.decorridoS > planta.tempoPadraoPedidoS

  return (
    <div className="dashboard-page">
      <PageHeader
        systemActive={!dash.error}
        breadcrumb="OPERAÇÃO / DASHBOARD OEE"
        eyebrow="MONITORAMENTO DE EFICIÊNCIA FABRIL"
        title="Visão geral da operação"
        subtitle="OEE de cada pedido da Bancada Smart 4.0 e o estado das estações agora."
        right={
          <>
            <select className="select-control" value={maquina.id} disabled aria-label="Máquina">
              <option value={maquina.id}>{maquina.nome}</option>
            </select>
            <select className="select-control" value={janela} onChange={(event) => setJanela(Number(event.target.value))}
              aria-label="Pedidos no gráfico de tendência">
              {JANELAS.map((n) => <option key={n} value={n}>Últimos {n} pedidos</option>)}
            </select>
          </>
        }
      />

      {aviso && <BannerAviso {...aviso} />}

      <div className="kpi-row">
        <KpiCard
          label="OEE DO ÚLTIMO PEDIDO"
          value={formatarPercentual(indicador?.oee)}
          caption={indicador
            ? `Meta ${formatarPercentual(metaOee, 0)}${abaixoDaMeta ? ' · abaixo da meta' : ' · meta atingida'}`
            : 'Nenhum pedido concluído ainda'}
          trend={tendencia(indicador?.oee, anteriores, 'oee')}
          progress={indicador?.oee ?? 0}
          featured
          alerta={abaixoDaMeta}
          refreshKey={`oee-${pedidoId}`}
        />
        <KpiCard
          label="DISPONIBILIDADE"
          value={formatarPercentual(indicador?.disponibilidade)}
          caption={`Tempo parado ${formatarDuracao(pedido?.tempoParadoS)}`}
          trend={tendencia(indicador?.disponibilidade, anteriores, 'disponibilidade')}
          refreshKey={`d-${pedidoId}`}
        />
        <KpiCard
          label="PERFORMANCE"
          value={formatarPercentual(indicador?.performance)}
          caption={`Lead time ${formatarDuracao(pedido?.leadTimeS)} · padrão ${formatarDuracao(pedido?.tempoPadraoS)}`}
          trend={tendencia(indicador?.performance, anteriores, 'performance')}
          alerta={pedido && pedido.leadTimeS - pedido.tempoParadoS > pedido.tempoPadraoS}
          refreshKey={`p-${pedidoId}`}
        />
        <KpiCard
          label="QUALIDADE"
          value={formatarPercentual(indicador?.qualidade)}
          caption={pedido ? `Pedido nº ${pedido.numero} ${pedido.reprovado ? 'reprovado' : 'aprovado'}` : '-'}
          trend={tendencia(indicador?.qualidade, anteriores, 'qualidade')}
          alerta={pedido?.reprovado}
          refreshKey={`q-${pedidoId}`}
        />
      </div>

      <div className="dash-top-row">
        <Panel
          title="TENDÊNCIA"
          right={
            <span className="dash-trend-legend">
              <span className="dot" style={{ background: 'var(--content-accent)' }} /> OEE por pedido
              <span className="dash-trend-meta" /> Meta
            </span>
          }
        >
          <div className="dash-trend-title">Evolução do OEE</div>
          {pontos.length
            ? <TrendChart points={pontos} meta={metaOee} />
            : <div className="dash-trend-vazio">O gráfico aparece quando o primeiro pedido for concluído.</div>}
        </Panel>

        <Panel
          title="STATUS DA LINHA"
          right={
            <span className={`status-online-pill${CLASSE_PILL[planta?.status] ?? ' status-paused'}`}>
              <span className={`dot dot-${corDoStatus(planta?.status)}`} /> {planta?.statusNome ?? 'Sem conexão'}
            </span>
          }
        >
          <div className="status-line-title">Agora</div>
          <div className="status-line-machine">
            <span className="status-line-machine-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <rect x="3" y="3" width="18" height="18" rx="3" />
                <rect x="9" y="9" width="6" height="6" rx="1" />
              </svg>
            </span>
            <div className="status-line-machine-info">
              <div className="status-line-machine-name">
                {emAndamento ? `Pedido nº ${emAndamento.numero ?? '-'}` : maquina.nome}
              </div>
              <div className={`status-line-machine-sub${acimaDoPadrao ? ' acima' : ''}`}>
                {emAndamento
                  ? `${emAndamento.etapaAtualNome ?? 'Iniciando'} · ${formatarDuracao(emAndamento.decorridoS)} de ${formatarDuracao(planta.tempoPadraoPedidoS)} padrão${acimaDoPadrao ? ' · acima do padrão' : ''}`
                  : planta?.conectado ? 'Aguardando pedido' : 'Sem leitura da bancada'}
              </div>
              {emAndamento && (
                <div className="andon-progress-track" aria-hidden="true">
                  <div className={`andon-progress-fill${acimaDoPadrao ? ' acima' : ''}`}
                    style={{ width: `${Math.min(100, (emAndamento.decorridoS / planta.tempoPadraoPedidoS) * 100)}%` }} />
                </div>
              )}
            </div>
          </div>

          <ul className="andon-estacoes" aria-label="Estações da bancada">
            {(planta?.estacoes ?? []).map((e) => (
              <li key={e.estacao} className="andon-estacao">
                <span className={`dot dot-${corDoStatus(e.situacao)}`} />
                <span className="andon-estacao-nome">{e.nome}</span>
                <span className="andon-estacao-status">{e.status ?? '—'}</span>
                {gargalo?.estacao === e.estacao
                  ? <span className="andon-estacao-extra atraso" title="Etapa que mais passou do padrão no último pedido">
                      +{formatarDuracao(gargalo.atrasoS)} no último pedido
                    </span>
                  : <span className="andon-estacao-extra">padrão {formatarDuracao(e.tempoPadraoS)}</span>}
              </li>
            ))}
          </ul>

          <div className="status-line-metrics">
            <div className="status-line-metric">
              <span>Pedidos hoje</span>
              <b>{resumoHoje?.concluidos ?? 0}</b>
            </div>
            <div className="status-line-metric">
              <span>Aprovados</span>
              <b>{resumoHoje?.aprovados ?? 0} de {resumoHoje?.concluidos ?? 0}</b>
            </div>
            <div className="status-line-metric">
              <span>Lead time médio</span>
              <b className="accent">{formatarDuracao(resumoHoje?.leadTimeMedioS)}</b>
            </div>
          </div>
        </Panel>
      </div>

      <p className="updated-note">
        {planta?.fonteNome ? `Fonte: ${planta.fonteNome}` : 'Sem leitura da bancada'} · atualizado às {formatarHora(dash.atualizadoEm)}
      </p>
    </div>
  )
}

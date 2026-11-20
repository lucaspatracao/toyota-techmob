/**
 * Adapters: um por DTO da API (backend/.../dto). Convertem o JSON cru para
 * o formato das telas, sem inventar campo nenhum:
 *   - datas ISO-8601 viram `Date`;
 *   - códigos (estação, origem, status) ganham um rótulo em português;
 *   - números continuam números (tempos em segundos, percentuais de 0 a 100).
 *     A formatação para exibição fica nas funções `formatar*` no fim do arquivo.
 *
 * O contrato de cada endpoint, com exemplos, está em backend/README.md.
 */

// ---------- Códigos da API e rótulos ----------

/** As três estações da bancada, na ordem em que o pedido passa por elas. */
export const ESTACOES = [
  { codigo: 'ESTOQUE', nome: 'Estoque' },
  { codigo: 'MANIPULACAO', nome: 'Manipulação' },
  { codigo: 'SEPARACAO', nome: 'Separação' },
]

const NOME_ESTACAO = Object.fromEntries(ESTACOES.map((e) => [e.codigo, e.nome]))

/** Origem do dado do pedido (cabeçalho X-TechMob-Fonte do simulador). */
export const ORIGENS = [
  { codigo: 'BANCADA', nome: 'Bancada' },
  { codigo: 'REPLAY', nome: 'Replay' },
  { codigo: 'SINTETICO', nome: 'Simulação' },
]

const NOME_ORIGEM = Object.fromEntries(ORIGENS.map((o) => [o.codigo, o.nome]))

/** Status da bancada como um todo (maquina.status_operacional). */
const NOME_STATUS_MAQUINA = {
  PRODUZINDO: 'Operando',
  AGUARDANDO: 'Aguardando',
  PARADO: 'Parada',
  SEM_CONEXAO: 'Sem conexão',
}

const NOME_SITUACAO_PEDIDO = {
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  INTERROMPIDO: 'Interrompido',
}

/**
 * Cor do ponto de status (classes .dot-* do sidebar.css), igual para a
 * máquina e para cada estação no Andon: verde operando, cinza aguardando,
 * vermelho parado, laranja sem conexão.
 */
const COR_STATUS = {
  PRODUZINDO: 'green',
  OPERANDO: 'green',
  AGUARDANDO: 'gray',
  PARADO: 'red',
  SEM_CONEXAO: 'orange',
}

export function corDoStatus(status) {
  return COR_STATUS[status] ?? 'gray'
}

// ---------- MaquinaDTO ----------
// { id, bancadaId, nome, statusAtual, ultimaLeituraEm }
export function adaptMaquina(dto) {
  if (!dto) return null
  return {
    id: dto.id,
    bancadaId: dto.bancadaId,
    nome: dto.nome,
    status: dto.statusAtual,
    statusNome: NOME_STATUS_MAQUINA[dto.statusAtual] ?? dto.statusAtual,
    ultimaLeituraEm: paraData(dto.ultimaLeituraEm),
  }
}

export function adaptMaquinas(lista) {
  return (lista ?? []).map(adaptMaquina)
}

// ---------- IndicadorOEEDTO ----------
// { maquinaId, pedidoId, disponibilidade, performance, qualidade, oee,
//   periodoInicio, periodoFim, calculadoEm }
export function adaptIndicador(dto) {
  if (!dto) return null
  return {
    maquinaId: dto.maquinaId,
    pedidoId: dto.pedidoId,
    disponibilidade: dto.disponibilidade,
    performance: dto.performance,
    qualidade: dto.qualidade,
    oee: dto.oee,
    inicio: paraData(dto.periodoInicio),
    fim: paraData(dto.periodoFim),
    calculadoEm: paraData(dto.calculadoEm),
  }
}

export function adaptIndicadores(lista) {
  return (lista ?? []).map(adaptIndicador)
}

// ---------- StatusPlantaDTO ----------
// { maquinaId, status, conectado, fonte, operacao,
//   estacoes: [{ estacao, status, situacao, tempoPadraoS }],
//   pedidoEmAndamento: { pedidoId, numero, inicio, decorridoS, etapaAtual } | null,
//   tempoPadraoPedidoS, ultimaLeituraEm }
export function adaptStatusPlanta(dto) {
  if (!dto) return null
  const pedido = dto.pedidoEmAndamento
  return {
    maquinaId: dto.maquinaId,
    status: dto.status,
    statusNome: NOME_STATUS_MAQUINA[dto.status] ?? dto.status,
    conectado: dto.conectado,
    fonte: dto.fonte,
    fonteNome: dto.fonte ? NOME_ORIGEM[dto.fonte] ?? dto.fonte : null,
    operacao: dto.operacao,
    estacoes: (dto.estacoes ?? []).map((e) => ({
      estacao: e.estacao,
      nome: NOME_ESTACAO[e.estacao] ?? e.estacao,
      // texto que a bancada envia (null sem conexão) e a leitura dele
      status: e.status,
      situacao: e.situacao,
      tempoPadraoS: e.tempoPadraoS,
    })),
    pedidoEmAndamento: pedido
      ? {
          pedidoId: pedido.pedidoId,
          numero: pedido.numero,
          inicio: paraData(pedido.inicio),
          decorridoS: pedido.decorridoS,
          etapaAtual: pedido.etapaAtual,
          etapaAtualNome: pedido.etapaAtual ? NOME_ESTACAO[pedido.etapaAtual] ?? pedido.etapaAtual : null,
        }
      : null,
    tempoPadraoPedidoS: dto.tempoPadraoPedidoS,
    ultimaLeituraEm: paraData(dto.ultimaLeituraEm),
  }
}

// ---------- ResumoPedidosDTO ----------
// { concluidos, aprovados, reprovados, interrompidos, pecasProduzidas,
//   taxaReprovacao, leadTimeMedioS, tempoParadoTotalS,
//   disponibilidade, performance, qualidade, oee }
// Já vem no formato das telas; médias, taxa e OEE vêm null sem pedido concluído.
export function adaptResumo(dto) {
  return dto ? { ...dto } : null
}

// ---------- DashboardResponseDTO ----------
// { maquina, indicadorAtual, planta, resumoHoje, metaOee }
export function adaptDashboard(dto) {
  if (!dto) return null
  return {
    maquina: adaptMaquina(dto.maquina),
    indicadorAtual: adaptIndicador(dto.indicadorAtual),
    planta: adaptStatusPlanta(dto.planta),
    resumoHoje: adaptResumo(dto.resumoHoje),
    metaOee: dto.metaOee,
  }
}

// ---------- PedidoDTO ----------
// { id, maquinaId, numero, origem, situacao, inicio, fim, leadTimeS,
//   tempoParadoS, tempoPadraoS, reprovado, pecasBoas, pecasDefeituosas,
//   duracaoEtapasS: { ESTOQUE, MANIPULACAO, SEPARACAO },
//   disponibilidade, performance, qualidade, oee }
// D, P, Q e OEE vêm null em pedido em andamento ou interrompido.
export function adaptPedido(dto) {
  if (!dto) return null
  return {
    id: dto.id,
    maquinaId: dto.maquinaId,
    numero: dto.numero,
    origem: dto.origem,
    origemNome: NOME_ORIGEM[dto.origem] ?? dto.origem,
    situacao: dto.situacao,
    situacaoNome: NOME_SITUACAO_PEDIDO[dto.situacao] ?? dto.situacao,
    inicio: paraData(dto.inicio),
    fim: paraData(dto.fim),
    leadTimeS: dto.leadTimeS,
    tempoParadoS: dto.tempoParadoS,
    tempoPadraoS: dto.tempoPadraoS,
    reprovado: dto.reprovado,
    pecasBoas: dto.pecasBoas,
    pecasDefeituosas: dto.pecasDefeituosas,
    duracaoEtapasS: dto.duracaoEtapasS ?? {},
    disponibilidade: dto.disponibilidade,
    performance: dto.performance,
    qualidade: dto.qualidade,
    oee: dto.oee,
  }
}

export function adaptPedidos(lista) {
  return (lista ?? []).map(adaptPedido)
}

// ---------- EtapaPedidoDTO ----------
// { estacao, inicio, fim, duracaoS, tempoParadoS, tempoPadraoS }
export function adaptEtapa(dto) {
  if (!dto) return null
  return {
    estacao: dto.estacao,
    nome: NOME_ESTACAO[dto.estacao] ?? dto.estacao,
    inicio: paraData(dto.inicio),
    fim: paraData(dto.fim),
    duracaoS: dto.duracaoS,
    tempoParadoS: dto.tempoParadoS,
    tempoPadraoS: dto.tempoPadraoS,
  }
}

// ---------- PedidoDetalheDTO ----------
// { pedido: PedidoDTO, etapas: [EtapaPedidoDTO] (em ordem de início) }
export function adaptPedidoDetalhe(dto) {
  if (!dto) return null
  return {
    pedido: adaptPedido(dto.pedido),
    etapas: (dto.etapas ?? []).map(adaptEtapa),
  }
}

// ---------- Formatação para exibição (pt-BR) ----------

/** 80 -> "80,0%"; nulo -> "-". */
export function formatarPercentual(valor, casas = 1) {
  if (valor === undefined || valor === null) return '-'
  return `${valor.toFixed(casas).replace('.', ',')}%`
}

/** Segundos para texto: 47.3 -> "47 s"; 125 -> "2 min 5 s"; nulo -> "-". */
export function formatarDuracao(segundos) {
  if (segundos === undefined || segundos === null) return '-'
  const total = Math.round(segundos)
  if (total < 60) return `${total} s`
  const minutos = Math.floor(total / 60)
  const resto = total % 60
  if (minutos < 60) return resto ? `${minutos} min ${resto} s` : `${minutos} min`
  const horas = Math.floor(minutos / 60)
  return minutos % 60 ? `${horas} h ${minutos % 60} min` : `${horas} h`
}

const doisDigitos = (n) => String(n).padStart(2, '0')

/** Date -> "17/11/2026" (fuso do navegador). */
export function formatarData(data) {
  if (!data) return '-'
  return `${doisDigitos(data.getDate())}/${doisDigitos(data.getMonth() + 1)}/${data.getFullYear()}`
}

/** Date -> "10:05:12"; sem segundos: "10:05". */
export function formatarHora(data, { segundos = true } = {}) {
  if (!data) return '-'
  const hora = `${doisDigitos(data.getHours())}:${doisDigitos(data.getMinutes())}`
  return segundos ? `${hora}:${doisDigitos(data.getSeconds())}` : hora
}

/** Date -> "17/11/2026 10:05"; com segundos: "17/11/2026 10:05:12". */
export function formatarDataHora(data, { segundos = false } = {}) {
  if (!data) return '-'
  return `${formatarData(data)} ${formatarHora(data, { segundos })}`
}

function paraData(iso) {
  return iso ? new Date(iso) : null
}

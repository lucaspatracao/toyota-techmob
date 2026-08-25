// Dados fictícios para desenvolvimento visual.
// Em produção, estes dados virão da API REST (Spring Boot) que lê do
// PostgreSQL, conforme a arquitetura definida no Documento do Projeto.

export const oeeSeries = [
  { time: '14:00', oee: 62 }, { time: '15:00', oee: 65 }, { time: '16:00', oee: 68 },
  { time: '17:00', oee: 66 }, { time: '18:00', oee: 70 }, { time: '19:00', oee: 72 },
  { time: '20:00', oee: 69 }, { time: '21:00', oee: 71 }, { time: '22:00', oee: 74 },
  { time: '23:00', oee: 73 }, { time: '00:00', oee: 75 }, { time: '01:00', oee: 74 },
  { time: '02:00', oee: 76 }, { time: '03:00', oee: 75 }, { time: '04:00', oee: 77 },
  { time: '06:00', oee: 76 }, { time: '08:00', oee: 78 }, { time: '10:00', oee: 77 },
  { time: '12:00', oee: 79 }, { time: '14:00', oee: 78.4 },
]

export const productionSeries = [
  { time: '14:00', boas: 40, rejeitadas: 4, taxa: 8 }, { time: '16:00', boas: 55, rejeitadas: 5, taxa: 7 },
  { time: '18:00', boas: 90, rejeitadas: 6, taxa: 5 }, { time: '20:00', boas: 70, rejeitadas: 8, taxa: 6 },
  { time: '22:00', boas: 100, rejeitadas: 5, taxa: 4 }, { time: '00:00', boas: 95, rejeitadas: 6, taxa: 5 },
  { time: '02:00', boas: 130, rejeitadas: 7, taxa: 4 }, { time: '04:00', boas: 120, rejeitadas: 6, taxa: 3 },
  { time: '06:00', boas: 110, rejeitadas: 8, taxa: 5 }, { time: '08:00', boas: 140, rejeitadas: 7, taxa: 4 },
  { time: '10:00', boas: 150, rejeitadas: 6, taxa: 3 }, { time: '12:00', boas: 160, rejeitadas: 8, taxa: 4 },
  { time: '14:00', boas: 155, rejeitadas: 9, taxa: 5 },
]

export const hourlyBars = Array.from({ length: 24 }, (_, i) => ({
  hour: String(i).padStart(2, '0'),
  boas: Math.round(80 + Math.random() * 120),
  rejeitadas: Math.round(5 + Math.random() * 20),
}))

export const periodSummary = [
  { periodo: '18/05/2024 10:00 - 14:00', boas: 356, rejeitadas: 18, taxa: '4,8%', ciclo: '8,21 s', oee: '79,2%' },
  { periodo: '18/05/2024 06:00 - 10:00', boas: 312, rejeitadas: 17, taxa: '5,2%', ciclo: '8,35 s', oee: '77,3%' },
  { periodo: '18/05/2024 02:00 - 06:00', boas: 264, rejeitadas: 13, taxa: '4,7%', ciclo: '8,61 s', oee: '76,1%' },
  { periodo: '17/05/2024 22:00 - 02:00', boas: 254, rejeitadas: 14, taxa: '5,2%', ciclo: '8,28 s', oee: '75,8%' },
  { periodo: '17/05/2024 18:00 - 22:00', boas: 248, rejeitadas: 13, taxa: '5,0%', ciclo: '8,46 s', oee: '74,2%' },
  { periodo: '17/05/2024 14:00 - 18:00', boas: 252, rejeitadas: 17, taxa: '6,3%', ciclo: '8,72 s', oee: '73,1%' },
]

export const historicoRows = [
  { status: 'green', dataHora: '18/05/2024 14:00 - 15:00', periodo: '14:00 - 15:00', turno: 'Tarde', boas: 356, rejeitadas: 18, taxa: '4,8%', ciclo: '8,21 s', oee: '79,2%' },
  { status: 'green', dataHora: '18/05/2024 13:00 - 14:00', periodo: '13:00 - 14:00', turno: 'Tarde', boas: 342, rejeitadas: 17, taxa: '4,7%', ciclo: '8,18 s', oee: '78,8%' },
  { status: 'green', dataHora: '18/05/2024 12:00 - 13:00', periodo: '12:00 - 13:00', turno: 'Tarde', boas: 298, rejeitadas: 15, taxa: '4,8%', ciclo: '8,33 s', oee: '77,6%' },
  { status: 'green', dataHora: '18/05/2024 11:00 - 12:00', periodo: '11:00 - 12:00', turno: 'Manhã', boas: 301, rejeitadas: 16, taxa: '5,0%', ciclo: '8,42 s', oee: '77,1%' },
  { status: 'green', dataHora: '18/05/2024 10:00 - 11:00', periodo: '10:00 - 11:00', turno: 'Manhã', boas: 312, rejeitadas: 17, taxa: '5,2%', ciclo: '8,35 s', oee: '77,3%' },
  { status: 'green', dataHora: '18/05/2024 09:00 - 10:00', periodo: '09:00 - 10:00', turno: 'Manhã', boas: 327, rejeitadas: 19, taxa: '5,5%', ciclo: '8,47 s', oee: '76,4%' },
  { status: 'orange', dataHora: '18/05/2024 08:00 - 09:00', periodo: '08:00 - 09:00', turno: 'Manhã', boas: 289, rejeitadas: 21, taxa: '6,8%', ciclo: '8,63 s', oee: '74,2%' },
  { status: 'green', dataHora: '17/05/2024 18:00 - 19:00', periodo: '18:00 - 19:00', turno: 'Tarde', boas: 248, rejeitadas: 13, taxa: '5,0%', ciclo: '8,46 s', oee: '74,2%' },
  { status: 'green', dataHora: '17/05/2024 17:00 - 18:00', periodo: '17:00 - 18:00', turno: 'Tarde', boas: 252, rejeitadas: 17, taxa: '6,3%', ciclo: '8,72 s', oee: '73,1%' },
  { status: 'red', dataHora: '17/05/2024 16:00 - 17:00', periodo: '16:00 - 17:00', turno: 'Tarde', boas: 198, rejeitadas: 22, taxa: '10,0%', ciclo: '9,21 s', oee: '68,5%' },
]

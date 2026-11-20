# Apresentação de slides: plano de ação

Atualizado em 20/11/2026 · Equipe TechMob 4.0

> Plano de ação da equipe para os slides da banca. Complementa o [Plano de alinhamento](../PLANO_ALINHAMENTO.md). Os outros entregáveis estão em [RELATORIO_TECNICO_ABNT.md](RELATORIO_TECNICO_ABNT.md) e [ROTEIRO_PITCH_TECNICO.md](ROTEIRO_PITCH_TECNICO.md).

## Objetivo

Montar um deck novo, do zero, que conte o projeto como ele está hoje: OEE por pedido, leitura do `/dadosPlanta`, simulador da bancada como fonte padrão e as três telas novas. O arquivo de 24/10 ("TechMob 4.0 — Projeto Integrador.pptx") descreve a arquitetura antiga e não é a base.

## Mensagem central

> O TechMob 4.0 mede a Bancada Smart 4.0 pelo OEE de cada pedido, a partir do contrato real da bancada e de um simulador fiel a ele: do dado à decisão.

Narrativa: **Contexto → Problema → Solução → Conclusão**. A solução é o software: ele monitora, analisa e apoia a decisão.

## Regras do deck

- **Linguagem (decisão de 19/11):** "contrato real da bancada" e "simulador da bancada". Nunca "dados reais da bancada", "lendo a bancada" ou "gravação real".
- **Um slide, uma ideia.** Título que afirma algo ("O OEE é calculado por pedido"), não só um rótulo ("OEE"). No máximo 3 tópicos curtos por slide; o resto vai na fala.
- **Números do repositório, sem arredondar para cima:** 80 s por pedido (20 + 35 + 25), meta de 75%, 72 testes no backend, 18 no Python, 6 endpoints GET, 5 tabelas.
- **Identidade visual:** as cores do próprio sistema (tokens em `frontend/src/styles/global.css`, como `--primary: #4338CA` e `--accent-green: #16A34A`), para o deck e o dashboard parecerem uma coisa só. Uma fonte sem serifa.
- **Rodapé:** "TechMob 4.0 · SAGA SENAI · nº do slide".
- **Duração:** a confirmar. O roteiro abaixo dá cerca de 11 min. Se o tempo for de 5 a 7 min, cortar os slides 9, 13 e 14 e juntar os três de demonstração em um.

## Roteiro slide a slide

| Nº | Título (proposta) | Conteúdo | Visual e fonte | Quem apresenta | Tempo |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | TechMob 4.0: do dado à decisão | Projeto Integrador, Faculdade de Tecnologia SENAI Antonio Adolpho Lobbe, curso de ADS, orientador, os 5 integrantes | Foto `bancada-frente.jpg` ao fundo, escurecida | Lucas | 0:20 |
| 2 | A indústria já é digital, mas falta gente para medir | Dois números da CNI (69% usam ao menos uma tecnologia digital; 60% sofrem com falta de mão de obra qualificada), com a fonte embaixo | Números grandes; fontes conferidas (ver o relatório) | Lucas | 0:40 |
| 3 | Sem medir, a perda fica invisível | Demanda da Fundação Toyota do Brasil e do SENAI-SP: aproximar a indústria da academia. A bancada produz pedidos de 3 peças, e ninguém sabia onde o tempo se perdia | Ícone de pergunta, frase de efeito | Lucas | 0:40 |
| 4 | Monitorar, analisar, decidir | O que o TechMob faz em uma frase por verbo; o indicador é o OEE por pedido | Três blocos com seta | Lucas | 0:40 |
| 5 | A Bancada Smart 4.0 | 3 estações (estoque, manipulação, separação), pedido de 3 peças, CLP Siemens ET 200SP | `bancada-estacoes.jpg`, `painel-et200sp.jpg` | Otávio | 0:45 |
| 6 | A bancada já publica o seu estado | `GET /dadosPlanta` no Node-RED da bancada, o JSON lido em 14/09, Node-RED só leitura. Por que não MQTT: o único MQTT sai para o TagoIO, e publicar para nós exigiria alterar o flow | `node-red-api-dados-planta.jpg`, `json-dadosplanta-navegador.jpg` | Otávio | 0:50 |
| 7 | Do JSON ao dashboard em 1 segundo | Simulador ou bancada → backend (PlantaClient → DetectorPedidos → CalculadoraOEE) → MySQL → React. O simulador responde o mesmo JSON; ligar a bancada é trocar a `PLANTA_URL` | Diagrama da arquitetura (exportado do README) | Lucas | 0:50 |
| 8 | O OEE é calculado por pedido | D = (L − T_parado) ÷ L; P = mín(1; T_padrão ÷ operando); Q = aprovado ou reprovado. Exemplo: pedido de 100 s, 12 s parado, padrão de 80 s → D 88%, P 90,9%, Q 100%, **OEE 80,0%** | Fórmula e uma barra do pedido dividida em operando e parado | Samuel | 0:50 |
| 9 | Um backend testado | Camadas Controller → Service → Repository; 6 endpoints GET; 5 tabelas; 72 testes JUnit e 18 pytest; Java e Python dão o mesmo OEE nas mesmas gravações de teste | Ícones das camadas e os números | Rafael | 0:50 |
| 10 | Dashboard: o Andon da bancada | As 3 estações ao vivo, o pedido em andamento contra o padrão, o OEE do último pedido contra a meta de 75% | Captura do Dashboard (ou demo ao vivo) | Samuel | 0:40 |
| 11 | Produção: onde o pedido atrasou | Tempo de cada etapa contra o padrão; a estação com maior diferença ganha o selo "gargalo" | Captura da Produção | Otávio | 0:40 |
| 12 | Histórico: o problema se repete? | Um pedido por linha, filtro por período e origem, detalhe com a linha do tempo das etapas | Captura do Histórico e do detalhe | Nykolas | 0:40 |
| 13 | O que entregamos | Os 4 eixos da demanda SAGA (monitoramento, análise, decisão, ambiente produtivo) e onde cada um aparece | Quadro do README (Objetivo) | Nykolas | 0:40 |
| 14 | Restrições e decisões | Node-RED da bancada só leitura; bancada didática, nenhum dado industrial ou pessoal; simulador como fonte (decisão de 19/11); qualidade por pedido | Quatro ícones de "check" | Nykolas | 0:40 |
| 15 | O sistema já fala a língua da bancada | Ligar é trocar uma URL. Próximos passos: gravar execuções reais, qualidade por peça | Foto da bancada e a frase | Lucas | 0:40 |
| 16 | Obrigado! Perguntas? | Link do repositório e QR code | QR code | Todos | — |

Tempo total: cerca de 11 min, sem as perguntas.

## Demonstração: ao vivo ou capturas

- **Plano A:** demo ao vivo entre os slides 9 e 13, se o ensaio completo funcionar no computador da apresentação.
- **Plano B (sempre pronto):** os slides 10 a 12 já trazem as capturas. Se a demo falhar, segue-se pelos slides sem parar.
- **Plano C:** o vídeo da banca ([ROTEIRO_PITCH_TECNICO.md](ROTEIRO_PITCH_TECNICO.md)) baixado no computador, para rodar sem internet.

## Material a produzir

| Item | Como | Quem |
| :---- | :---- | :---- |
| Capturas das telas | Ensaio completo com os 4 passos do README (schema, simulador, backend, frontend); deixar o simulador rodando cerca de 30 min para o Histórico ter 10 a 15 pedidos; navegador em tela cheia, 1920×1080, zoom 100%, o mesmo tema (claro ou escuro) em todas | Samuel (Dashboard), Otávio (Produção), Nykolas (Histórico) |
| Diagrama da arquitetura | Colar o bloco mermaid do README em mermaid.live e exportar em SVG ou PNG | Lucas |
| Fotos | `docs/img/` (já prontas) | — |
| QR code do repositório | Qualquer gerador de QR code a partir do link do GitHub | Nykolas |
| Dados da CNI | Fontes conferidas na publicação original (as mesmas do relatório) | Lucas |

## O que vem do deck de 24/10

| Aproveitar | Não usar |
| :---- | :---- |
| Dados da capa (instituição, curso, orientador, equipe) | Tudo que fala de MQTT ("prontos para MQTT", "ligar a bancada via MQTT") |
| Os dois números da CNI, depois de conferir as fontes | Capturas das telas antigas ("Meta do turno", filtros de 1H a 30D, "720 registros", agrupamento por turno) |
| A frase "Do dado à decisão" | O texto provisório "[confirmar: sem dados pessoais]" |
| | A lista de tecnologias antiga (Java 17, Chart.js, NumPy); a atual é Java 25, Spring Boot, React, Vite, Recharts, MySQL 8 e Python |

## Perguntas prováveis da banca

| Pergunta | Resposta curta | Quem responde |
| :---- | :---- | :---- |
| Os dados são reais? | O contrato é real: o JSON, o flow do Node-RED e o CLP são da bancada (fotos de 14/09). Os tempos são a cronometragem da equipe. Na apresentação, o simulador responde o mesmo JSON, e ligar a bancada é trocar a `PLANTA_URL` | Lucas |
| Por que não usaram MQTT? | O único MQTT da bancada é de saída para o TagoIO. Receber por MQTT exigiria alterar o flow da bancada, o que não é permitido; o `GET /dadosPlanta` já existe e é só leitura | Otávio |
| Por que OEE por pedido e não por turno? | A bancada trabalha por pedidos de 3 peças, não em turnos de 8 h. A fórmula é a mesma da fábrica; muda só a janela | Samuel |
| Como funciona a Qualidade? | Por pedido: se o contador de reprovados subiu durante o pedido, Q = 0 e o OEE do pedido é 0%. Dá para refinar se a bancada informar a reprovação por peça | Samuel |
| O que falta para ligar na bancada real? | Trocar a `PLANTA_URL`, conferir os textos de status (só `AGUARDANDO` foi lido na bancada) e ter acesso à rede do laboratório | Lucas |
| Por que o OEE existe em Java e em Python? | O backend é a única fonte do OEE no sistema. O Python confere: os testes rodam as mesmas gravações nos dois e comparam os números | Rafael |
| Como sabem que o cálculo está certo? | 72 testes no backend e 18 no Python, incluindo o exemplo de 80,0%, pedido reprovado e pedido interrompido | Rafael |
| E se a conexão cair no meio de um pedido? | Mais de 10 s sem conexão com pedido aberto: o pedido é gravado como interrompido e fica fora do OEE. Depois de 3 falhas seguidas, o backend passa a ler o simulador de reserva | Lucas |
| E a segurança e a LGPD? | Nenhum dado pessoal; a API só tem GET; o CORS só libera GET e OPTIONS; a senha do banco vem de variável de ambiente | Rafael |

## Cronograma

B é a data da banca.

| Prazo | Etapa | Responsável |
| :---- | :---- | :---- |
| B−14 | Roteiro aprovado pela equipe (este documento); duração confirmada | Lucas |
| B−10 | Ensaio completo do sistema e capturas das telas | Samuel, Otávio e Nykolas |
| B−7 | Deck montado com textos e imagens | Cada um nos seus slides; consolidação: Nykolas |
| B−5 | Primeiro ensaio cronometrado da apresentação | Todos |
| B−2 | Ensaio final no computador da apresentação, com a demo e os planos B e C | Todos |
| B | Apresentação | Todos |

## Checklist final

- [ ] Nenhum slide fala em MQTT como caminho do dado, em OEE por turno ou em "dados reais da bancada"
- [ ] Números conferidos com o repositório (80 s, 75%, 72 + 18 testes, 6 endpoints, 5 tabelas)
- [ ] Fontes da CNI conferidas e citadas no slide
- [ ] Capturas atuais, todas no mesmo tema e na mesma resolução
- [ ] Demo testada no computador da apresentação; capturas e vídeo prontos como reserva
- [ ] Cada integrante sabe os seus slides e as perguntas que responde
- [ ] Tempo total dentro do limite, medido em ensaio
- [ ] Deck exportado também em PDF

## Pendências

- Data da banca e duração da apresentação.
- Se a banca quer a demonstração ao vivo ou aceita as capturas.
- Computador e projetor da apresentação (para o ensaio final).

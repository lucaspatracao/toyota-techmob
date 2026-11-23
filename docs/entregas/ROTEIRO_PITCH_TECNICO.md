# Roteiro do vídeo pitch técnico: plano de ação

Atualizado em 23/11/2026 · Equipe TechMob 4.0

> Plano de ação da equipe para os vídeos do projeto. Complementa o [Plano de alinhamento](../PLANO_ALINHAMENTO.md). Os outros entregáveis estão em [RELATORIO_TECNICO_ABNT.md](RELATORIO_TECNICO_ABNT.md) e [TechMob_4.0_Apresentacao.pptx](TechMob_4.0_Apresentacao.pptx) (os slides).
>
> **Base do vídeo:** o deck [`TechMob_4.0_Apresentacao.pptx`](TechMob_4.0_Apresentacao.pptx) (16 slides). Cada cena do vídeo é um slide, e a narração é a mesma das notas do orador. Assim a equipe ensaia uma vez só, e o vídeo serve de reserva exata da apresentação ao vivo.

## A demanda que o vídeo responde

O vídeo responde à demanda do Projeto Integrador 2026/02 cadastrada no SAGA, na mesma ordem do deck: contexto → demanda → problema → solução → demonstração → resultado → conclusão.

| Item da demanda | Onde aparece no vídeo |
| :---- | :---- |
| Programa TechMob 4.0, "Programe o futuro" (Fundação Toyota do Brasil + SENAI-SP) | Slide 2 (por que o programa existe) e slide 3 |
| Objetivo principal: soluções digitais para monitoramento, análise de dados e tomada de decisão | Slide 3 |
| Eixo 1: monitoramento de processos | Slides 5, 9 e 12 |
| Eixo 2: análise de dados | Slides 5, 6, 10, 11 e 12 |
| Eixo 3: apoio à tomada de decisão | Slides 5, 9, 10 e 12 |
| Eixo 4: aplicação em ambientes produtivos | Slides 4, 5, 7 e 12 |
| As 4 restrições (calendário, sem dados confidenciais, LGPD, tecnologias acessíveis) | Slide 13; calendário e tecnologias acessíveis retomados no slide 15 |
| Benefícios esperados | Slide 14 |
| Resposta à demanda ("sim, é possível") | Slide 15 |

## Objetivo e versões

| Versão | Duração | Para quê |
| :---- | :---- | :---- |
| Banca | Meta de 4:20 (limite de 3 a 5 min) | Os 16 slides, na ordem, com a narração das notas e a gravação de tela nos slides 9 a 11. É a reserva da apresentação se a demo ao vivo falhar |
| SAGA | Cerca de 1:30 | Corte com 8 slides para quem avalia a demanda: demanda, problema, solução, Dashboard, gargalo, eixos e restrições, conclusão |

A fala ao vivo dura cerca de 4:10 (cerca de 4:30 com as trocas de slide, conforme a nota do slide 16). No vídeo as trocas são cortes, então a meta fica em 4:20.

Os dois vídeos usam os slides exportados como imagem e a gravação de tela do sistema rodando com o simulador da bancada. Não há gravação no laboratório nem replay (decisão de 19/11).

## Regras

- **Narração = nota do slide:** a fala do vídeo é a mesma das notas do orador. Se a equipe mudar uma, muda a outra no mesmo dia.
- **Quem fala é o dono do slide:** Lucas (1, 2, 3, 15), Otávio (4, 12, 13), Samuel (5, 6, 14), Rafael (7, 8) e Nykolas (9, 10, 11).
- **Honestidade sobre a fonte:** o selo "Simulação" fica visível na barra superior das gravações de tela, e a narração fala em "simulador" e "contrato real da bancada". Nunca "dados reais da bancada".
- **Números do deck e do repositório:** 80 s por pedido (20 + 35 + 25), meta de 75%, exemplo de OEE de 80,0% (D 88,0%, P 90,9%, Q 100%), 6 endpoints GET, 5 tabelas, 72 + 18 testes.
- **Dados da CNI (slide 2):** 69% (Sondagem Especial nº 83, dados de 2021) e 50% (Sondagem Especial nº 76, dados de 2019). As fontes estão na nota do slide. Conferir e anotar a data de acesso antes da exportação final.
- **LGPD no próprio vídeo:** nenhum dado pessoal na tela, autorização por escrito dos integrantes para uso de voz e imagem, fotos sem pessoas de fora da equipe (a do slide 4 já está recortada).
- **Ferramentas acessíveis também na produção:** OBS, editor gratuito e trilha sem direitos autorais, coerente com a restrição 4 (slide 13).
- **Vozes:** cada fala gravada separada, em ambiente silencioso, com o mesmo microfone se possível.
- **Legendas:** sempre, porque o vídeo pode ser visto sem som.

## Preparação da gravação

1. **Conferir a fala de LGPD:** antes de gravar os slides 13 e 16, confirmar no `infra/schema.sql` e nos logs do backend que nenhuma tabela ou log guarda dado pessoal.
2. **Exportar os slides:** no PowerPoint, Arquivo › Exportar › PNG, 1920×1080, todos os slides. O deck é 16:9, então a imagem preenche a tela sem bordas.
3. **Subir o sistema** com os 4 passos do README: `infra/schema.sql` no MySQL, `python data/simulador_planta.py`, `backend\run-local.cmd` e `npm run dev` no frontend.
4. **Deixar rodar cerca de 30 min antes de gravar.** Cada pedido leva cerca de 2 min, então o Histórico terá de 10 a 15 pedidos e os gráficos não aparecem vazios.
5. **Tela:** navegador em tela cheia, 1920×1080, zoom 100%, **tema claro** (o mesmo das capturas dos slides 9 a 11), sem abas, favoritos ou notificações.
6. **Gravação de tela:** OBS Studio (gratuito), 1080p, 30 fps. Um arquivo por tela (Dashboard, Produção, Histórico), com movimentos de mouse lentos. É o mesmo ensaio que gera as capturas estáticas dos slides 9 a 11.
7. **Momentos a capturar:**
   - Dashboard: OEE do último pedido contra a meta, as 3 estações mudando de estado, um pedido em andamento e o alerta de pedido acima do padrão.
   - Produção: tempos por etapa, média por estação e o selo "gargalo".
   - Histórico: a lista com D, P, Q e OEE, o filtro por período e origem, e a abertura do detalhe com a linha do tempo das 3 etapas.

## Roteiro da versão da banca (4:20)

| Tempo | Slide | Cena no vídeo | Narração (nota do slide) | Quem |
| :---- | :---- | :---- | :---- | :---- |
| 0:00–0:15 | 1 · Título | Slide parado, com zoom lento | "Somos a equipe TechMob 4.0, do Projeto Integrador de ADS da Faculdade de Tecnologia SENAI Antonio Adolpho Lobbe, com orientação do Prof. André Roberto. Nosso projeto: do dado à decisão." | Lucas |
| 0:15–0:30 | 2 · Contexto | Entram o 69%, depois o 50%, depois o bloco do programa | "A indústria já é digital: 69% das indústrias usam alguma tecnologia digital. Mas metade enfrenta falta de trabalhador qualificado. Por isso a Fundação Toyota do Brasil e o SENAI-SP criaram o programa TechMob 4.0." | Lucas |
| 0:30–0:45 | 3 · A demanda | Objetivo principal; os 4 eixos entram um a um | "A demanda do programa é clara: soluções digitais para monitoramento, análise de dados e tomada de decisão em ambientes industriais, nesses 4 eixos. O software não é a demanda: é a nossa resposta. O Otávio mostra onde aplicamos." | Lucas |
| 0:45–1:05 | 4 · Problema | Foto da bancada; os "? s" das estações piscam; no fim, a linha do padrão de 80 s | "Escolhemos um processo real do laboratório: a Bancada Smart 4.0, com CLP Siemens. Cada pedido de 3 peças passa por estoque, manipulação e separação, com padrão cronometrado de 80 segundos. Mas ninguém sabia onde o tempo se perdia. Sem medir, a perda fica invisível." | Otávio |
| 1:05–1:20 | 5 · Solução | Destaque em Monitorar, Analisar e Decidir, na ordem da fala; por último a faixa do 4º eixo | "O TechMob responde a cada eixo com um verbo. Monitorar: as 3 estações ao vivo. Analisar: o OEE de cada pedido. Decidir: alerta quando o pedido passa do padrão ou o OEE fica abaixo da meta. E tudo parte do contrato real da bancada." | Samuel |
| 1:20–1:40 | 6 · O indicador | D, P e Q entram na ordem; depois o OEE de 80,0% e a barra do pedido | "O indicador é o OEE, o mesmo da fábrica, mas por pedido. Exemplo: um pedido de 100 segundos, com 12 parados, contra o padrão de 80. Disponibilidade 88%, Performance 90,9%, Qualidade 100%: OEE de 80%, acima da meta de 75%." | Samuel |
| 1:40–2:00 | 7 · Arquitetura | Destaque segue o caminho do dado: fonte → PlantaClient → DetectorPedidos → CalculadoraOEE → MySQL → React | "Por dentro: a bancada publica o seu estado no Node-RED, e nós só lemos. Hoje a fonte é o simulador, com o mesmo JSON: ligar a bancada é trocar uma URL. O backend lê a cada segundo, detecta cada pedido, calcula o OEE e grava no MySQL; a API alimenta o React." | Rafael |
| 2:00–2:15 | 8 · Back-end e testes | Camadas; depois os 4 números (6, 5, 72, 18) | "O backend segue Controller, Service e Repository: 6 endpoints, todos GET, e 5 tabelas. E o cálculo é conferido por 72 testes no backend e 18 no Python: Java e Python dão o mesmo OEE." | Rafael |
| 2:15–2:35 | 9 · Dashboard | Gravação de tela dentro do quadro da captura; os números 1, 2 e 3 acendem com a fala; o alerta aparece na gravação | "Na prática, cada tela responde a uma pergunta. O que acontece agora? O Dashboard mostra o OEE do último pedido contra a meta, as 3 estações ao vivo e o pedido em andamento contra o padrão. Se passar do padrão ou da meta, acende o alerta." | Nykolas |
| 2:35–2:50 | 10 · Produção | Gravação de tela no quadro; termina no selo "gargalo" | "Onde o pedido atrasou? A Produção mostra o tempo de cada etapa, pedido a pedido, e a média de cada estação contra o padrão. A que mais passa ganha o selo de gargalo: é ali que se age primeiro." | Nykolas |
| 2:50–3:05 | 11 · Histórico | Gravação de tela no quadro: lista, filtro e abertura do detalhe | "O problema se repete? O Histórico lista cada pedido com D, P, Q e OEE, filtra por período e pela origem do dado, e o detalhe mostra a linha do tempo das 3 etapas." | Nykolas |
| 3:05–3:20 | 12 · Resultado | As 4 linhas da tabela acendem uma a uma | "Voltando à demanda: os 4 eixos estão no sistema. Monitoramento no Dashboard, análise na Produção e no Histórico, decisão nos alertas e no gargalo, e o ambiente produtivo é o contrato real da bancada." | Otávio |
| 3:20–3:35 | 13 · Restrições | Os 4 blocos acendem na ordem da fala | "E as 4 restrições foram respeitadas: escopo que cabe no semestre, sem dado confidencial, sem dado pessoal e com API só de leitura, e só tecnologias gratuitas e de código aberto." | Otávio |
| 3:35–3:50 | 14 · Benefícios | Os 4 benefícios entram um a um | "A demanda também quer formar profissionais. Na prática, trabalhamos com tecnologia industrial de verdade, decidimos pelo indicador, usamos uma stack de mercado com testes, e tudo roda com ferramentas gratuitas, até sem a bancada." | Samuel |
| 3:50–4:10 | 15 · Conclusão | Título "Sim, é possível"; depois os 3 próximos passos | "Então, a resposta à demanda: sim, é possível. Com tecnologias acessíveis e dentro do calendário, o TechMob mede onde o tempo se perde em cada pedido. Próximos passos: ligar o backend à bancada, validar o OEE com execuções nela e medir a qualidade por peça. Obrigado." | Lucas |
| 4:10–4:20 | 16 · Obrigado | Slide parado com o QR code; trilha sobe e encerra | Sem narração | — |

Tempo de fala por pessoa: Lucas 1:05, Otávio 0:50, Nykolas 0:50, Samuel 0:50 e Rafael 0:35.

**Slides 9 a 11 no vídeo:** em vez da captura estática, a gravação de tela entra no lugar do quadro da captura, com o resto do slide (título, números 1-2-3 e o selo de alerta, gargalo ou meta) por cima. Assim o vídeo mostra o sistema rodando sem perder a explicação do slide.

**Se a fala passar do tempo:** estender o slide (não acelerar o áudio). O total pode chegar a 5:00.

## Corte para a SAGA (1:30)

Oito slides, com falas condensadas das notas. Cada fala é do dono do slide.

| Tempo | Slide | Cena | Narração | Quem |
| :---- | :---- | :---- | :---- | :---- |
| 0:00–0:05 | 1 · Título | Slide parado | "Somos a equipe TechMob 4.0: do dado à decisão." | Lucas |
| 0:05–0:20 | 3 · A demanda | Objetivo e os 4 eixos | "O programa TechMob 4.0, da Fundação Toyota do Brasil com o SENAI-SP, pede soluções digitais para monitorar processos, analisar dados e apoiar decisões em ambientes industriais." | Lucas |
| 0:20–0:32 | 4 · Problema | Foto da bancada e o padrão de 80 s | "Escolhemos a Bancada Smart 4.0, com CLP Siemens: cada pedido tem padrão de 80 segundos, mas ninguém sabia onde o tempo se perdia." | Otávio |
| 0:32–0:47 | 5 · Solução | Monitorar, Analisar, Decidir e a faixa do 4º eixo | "Nossa resposta: monitorar as 3 estações ao vivo, analisar o OEE de cada pedido e decidir pelo alerta, tudo a partir do contrato real da bancada." | Samuel |
| 0:47–0:59 | 9 · Dashboard | Gravação de tela no quadro, com o alerta | "No Dashboard, as estações ao vivo e o OEE contra a meta de 75%. Passou do padrão, acende o alerta." | Nykolas |
| 0:59–1:07 | 10 · Produção | Gravação de tela terminando no selo "gargalo" | "Na Produção, o gargalo mostra onde agir primeiro." | Nykolas |
| 1:07–1:20 | 12 e 13 · Resultado e restrições | Slide 12 por 6 s, slide 13 por 7 s | "Os 4 eixos da demanda estão no sistema, e as 4 restrições foram respeitadas: sem dado confidencial, sem dado pessoal e só tecnologias gratuitas." | Otávio |
| 1:20–1:30 | 15 e 16 · Conclusão | Título "Sim, é possível"; corta para o QR code | "Sim, é possível aproximar a escola da indústria. TechMob 4.0: do dado à decisão." | Lucas |

## Pós-produção

| Etapa | Como | Quem |
| :---- | :---- | :---- |
| Montagem | Editor gratuito (DaVinci Resolve, Shotcut ou Clipchamp): PNGs dos slides na linha do tempo, gravação de tela encaixada no quadro dos slides 9 a 11, destaques acompanhando a fala | Samuel |
| Áudio | Normalizar o volume das falas; trilha de fundo baixa e sem direitos autorais (por exemplo, a biblioteca de áudio do YouTube), ou nenhuma | Samuel |
| Legendas | Texto da narração (as notas do deck), conferido com o vídeo final | Nykolas |
| Conferência | Duração (4:20 e 1:30), narração igual às notas, números, selo "Simulação" visível, nada de "dados reais" | Lucas |
| Exportação | MP4 (H.264), 1080p, 30 fps; nomes `TechMob40_Banca.mp4` e `TechMob40_SAGA.mp4` | Samuel |

## Cronograma

B é a data da banca (ou a data de entrega do vídeo, se vier antes).

| Prazo | Etapa | Responsável |
| :---- | :---- | :---- |
| B−14 | Deck e notas congelados (a partir daqui, mudança só em conjunto com o vídeo); fontes da CNI conferidas | Lucas |
| B−12 | Conferência de dado pessoal no `schema.sql` e nos logs; autorizações de voz e imagem assinadas | Rafael; autorizações: Lucas |
| B−10 | Ensaio completo: gravação de tela e capturas estáticas dos slides 9 a 11 no mesmo ensaio; slides exportados em PNG | Samuel e Nykolas |
| B−8 | Gravação das narrações | Cada dono de slide |
| B−6 | Primeira montagem da versão da banca | Samuel |
| B−4 | Revisão da equipe; legendas | Todos; legendas: Nykolas |
| B−3 | Corte da SAGA e versões finais exportadas | Samuel |
| B−2 | Vídeo salvo no computador da apresentação (reserva da demo) | Lucas |

## Checklist final

- [ ] Versão da banca entre 3 e 5 min (meta de 4:20); versão da SAGA perto de 1:30
- [ ] Os 16 slides na ordem do deck, cada um com a fala do seu dono
- [ ] Narração igual às notas do orador do deck
- [ ] Placeholders cinza dos slides 9 a 11 trocados pela captura no deck e pela gravação de tela no vídeo
- [ ] Fontes da CNI do slide 2 conferidas, com data de acesso anotada
- [ ] Fala "nenhum dado pessoal" (slide 13) confirmada no `schema.sql` e nos logs
- [ ] Autorização de uso de voz e imagem de todos os integrantes
- [ ] Selo "Simulação" visível nas gravações; nada de "dados reais da bancada"
- [ ] Números conferidos com o deck: 80 s (20 + 35 + 25), meta de 75%, OEE de 80,0%, 6 endpoints, 5 tabelas, 72 + 18 testes
- [ ] Áudio limpo e no mesmo volume; legendas conferidas
- [ ] Nenhuma notificação, aba ou dado pessoal na tela
- [ ] MP4 1080p exportado e assistido do início ao fim
- [ ] Cópia offline no computador da apresentação

## Pendências

- Se o SAGA ou a banca têm critérios de avaliação próprios (rubrica) além do texto da demanda.
- Formato e local de entrega dos vídeos (plataforma, link, arquivo).
- Duração exata exigida para cada versão.
- Se os integrantes precisam aparecer no vídeo ou se basta a narração.

# Roteiro do vídeo pitch técnico: plano de ação

Atualizado em 20/11/2026 · Equipe TechMob 4.0

> Plano de ação da equipe para os vídeos do projeto. Complementa o [Plano de alinhamento](../PLANO_ALINHAMENTO.md). Os outros entregáveis estão em [RELATORIO_TECNICO_ABNT.md](RELATORIO_TECNICO_ABNT.md) e [APRESENTACAO_SLIDES.md](APRESENTACAO_SLIDES.md).

## Objetivo e versões

| Versão | Duração | Para quê |
| :---- | :---- | :---- |
| Banca | Meta de 4:00 (limite de 3 a 5 min) | Pitch técnico completo, com a demonstração das três telas. Também é a reserva da apresentação se a demo ao vivo falhar |
| SAGA | Cerca de 1:30 | Corte da versão da banca: problema, bancada, Andon e OEE, conclusão |

Os dois vídeos usam a gravação de tela do dashboard rodando com o simulador da bancada, as fotos do laboratório de 14/09 e o diagrama da arquitetura. Não há gravação no laboratório nem replay (decisão de 19/11).

## Regras

- **Honestidade sobre a fonte:** o selo "Simulação" fica visível na barra superior durante a demo, e a narração fala em "simulador da bancada". Nunca "dados reais da bancada".
- **Números do repositório:** 80 s por pedido, meta de 75%, exemplo de OEE de 80,0%.
- **Vozes:** cada integrante narra a parte da sua função (como na tabela abaixo). Gravar cada fala separada, em ambiente silencioso, com o mesmo microfone se possível.
- **Legendas:** sempre, porque o vídeo pode ser visto sem som.
- **Identidade:** mesmas cores e fonte dos slides.

## Preparação da gravação

1. **Subir o sistema** com os 4 passos do README: `infra/schema.sql` no MySQL, `python data/simulador_planta.py`, `backend\run-local.cmd` e `npm run dev` no frontend.
2. **Deixar rodar cerca de 30 min antes de gravar.** Cada pedido leva cerca de 2 min (80 s de padrão, mais paradas eventuais e 30 s de intervalo), então o Histórico terá de 10 a 15 pedidos e os gráficos não aparecem vazios.
3. **Tela:** navegador em tela cheia, 1920×1080, zoom 100%, sem abas, favoritos ou notificações; o mesmo tema (claro ou escuro) dos slides.
4. **Gravação de tela:** OBS Studio (gratuito), 1080p, 30 fps. Uma cena por arquivo (Dashboard, Produção, Histórico), com movimentos de mouse lentos.
5. **Momentos a capturar:** um pedido em andamento no Andon; um pedido acima do padrão (alerta); o selo "gargalo" na Produção; a abertura do detalhe de um pedido no Histórico.

## Roteiro da versão da banca (4:00)

| Tempo | Cena (o que aparece) | Narração | Quem |
| :---- | :---- | :---- | :---- |
| 0:00–0:15 | Título "TechMob 4.0: do dado à decisão" sobre a foto da bancada | "Na Indústria 4.0, quem não mede não melhora. Somos a equipe TechMob 4.0, e este é o nosso projeto para a Bancada Smart 4.0." | Lucas |
| 0:15–0:40 | Fotos da bancada; texto "Demanda: Fundação Toyota do Brasil + SENAI-SP" | "A bancada produz pedidos de três peças em três estações. Mas ninguém sabia quanto tempo cada etapa levava nem onde o tempo se perdia. Sem medir, a perda fica invisível." | Lucas |
| 0:40–1:10 | Painel ET 200SP, flow do Node-RED e o JSON no navegador | "O CLP Siemens ET 200SP publica o estado da bancada num Node-RED, na rota /dadosPlanta. O nosso sistema só lê esse JSON: nada é instalado ou alterado na bancada." | Otávio |
| 1:10–1:40 | Diagrama da arquitetura, com destaque em cada bloco | "A cada segundo, o backend em Spring Boot lê o JSON, identifica cada pedido pelas mudanças de estado, calcula o OEE e grava no MySQL. Nesta demonstração, a fonte é o simulador da bancada, que responde o mesmo JSON com os tempos da nossa cronometragem. Para ligar na bancada, basta trocar uma URL." | Rafael |
| 1:40–2:10 | Fórmula do OEE e o exemplo do pedido de 100 s | "O OEE multiplica disponibilidade, performance e qualidade, só que por pedido, e não por turno. Num pedido de 100 segundos, com 12 parados e padrão de 80, o OEE é de 80%." | Samuel |
| 2:10–2:50 | Gravação de tela: Dashboard (Andon, pedido em andamento, OEE) | "No Dashboard, o Andon mostra as três estações ao vivo e o pedido em andamento contra o tempo padrão. O OEE do último pedido aparece comparado à meta de 75%." | Samuel |
| 2:50–3:20 | Gravação de tela: Produção (barras por etapa, selo "gargalo") | "Na Produção, cada pedido aparece etapa por etapa, contra o padrão. A estação que mais atrasa ganha o selo de gargalo: é ali que vale agir." | Otávio |
| 3:20–3:45 | Gravação de tela: Histórico e o detalhe de um pedido | "No Histórico, filtramos os pedidos por período e origem e abrimos o detalhe para ver em qual etapa o tempo foi perdido." | Nykolas |
| 3:45–4:00 | Equipe, link do repositório e QR code | "Do dado à decisão: o TechMob 4.0 já fala a língua da bancada. Obrigado!" | Lucas |

## Corte para a SAGA (1:30)

| Tempo | Cena | Narração (condensada da versão da banca) | Quem |
| :---- | :---- | :---- | :---- |
| 0:00–0:10 | Título sobre a foto da bancada | "Somos a equipe TechMob 4.0, e medimos a Bancada Smart 4.0 do dado à decisão." | Lucas |
| 0:10–0:25 | Fotos da bancada | "A bancada produz pedidos de três peças, mas ninguém sabia onde o tempo se perdia." | Lucas |
| 0:25–0:45 | Node-RED, JSON e diagrama | "Lemos o estado da bancada sem alterar nada nela e calculamos o OEE de cada pedido. Na demonstração, um simulador responde o mesmo JSON da bancada." | Otávio |
| 0:45–1:05 | Dashboard: Andon e OEE | "O Andon mostra as estações ao vivo, e o OEE de cada pedido é comparado à meta." | Samuel |
| 1:05–1:20 | Produção: selo "gargalo" | "A estação gargalo fica em destaque: é ali que vale agir." | Otávio |
| 1:20–1:30 | Equipe e link | "TechMob 4.0: do dado à decisão." | Lucas |

## Pós-produção

| Etapa | Como | Quem |
| :---- | :---- | :---- |
| Montagem | Editor gratuito (DaVinci Resolve, Shotcut ou Clipchamp), seguindo as tabelas acima | Samuel |
| Áudio | Normalizar o volume das falas; trilha de fundo baixa e sem direitos autorais (por exemplo, a biblioteca de áudio do YouTube), ou nenhuma | Samuel |
| Legendas | Texto da narração, conferido com o vídeo final | Nykolas |
| Conferência | Duração (4:00 e 1:30), números, selo "Simulação" visível, nada de "dados reais" | Lucas |
| Exportação | MP4 (H.264), 1080p, 30 fps; nomes `TechMob40_Banca.mp4` e `TechMob40_SAGA.mp4` | Samuel |

## Cronograma

B é a data da banca (ou a data de entrega do vídeo, se vier antes).

| Prazo | Etapa | Responsável |
| :---- | :---- | :---- |
| B−14 | Roteiro aprovado e falas distribuídas | Lucas |
| B−10 | Gravação de tela (no mesmo ensaio das capturas dos slides) | Samuel |
| B−8 | Gravação das narrações | Cada narrador |
| B−6 | Primeira montagem da versão da banca | Samuel |
| B−4 | Revisão da equipe; legendas | Todos; legendas: Nykolas |
| B−3 | Corte da SAGA e versões finais exportadas | Samuel |
| B−2 | Vídeo salvo no computador da apresentação (reserva da demo) | Lucas |

## Checklist final

- [ ] Versão da banca entre 3 e 5 min (meta de 4:00); versão da SAGA perto de 1:30
- [ ] Selo "Simulação" visível na demo; narração fala em "simulador da bancada"
- [ ] Números conferidos: 80 s por pedido, meta de 75%, OEE de 80,0% no exemplo
- [ ] Áudio limpo e no mesmo volume; legendas conferidas
- [ ] Nenhuma notificação, aba ou dado pessoal aparecendo na tela
- [ ] MP4 1080p exportado e assistido do início ao fim
- [ ] Cópia offline no computador da apresentação

## Pendências

- Formato e local de entrega dos vídeos (plataforma, link, arquivo).
- Duração exata exigida para cada versão.
- Se os integrantes precisam aparecer no vídeo ou se basta a narração.

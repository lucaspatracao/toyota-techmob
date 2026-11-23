# Relatório técnico (ABNT): plano de ação

Atualizado em 23/11/2026 · Equipe TechMob 4.0

> Plano de ação da equipe para o relatório técnico do TechMob 4.0. Complementa o [Plano de alinhamento](../PLANO_ALINHAMENTO.md), que é a fonte das decisões, do contrato e da arquitetura. Os outros entregáveis estão em [TechMob_4.0_Apresentacao.pptx](TechMob_4.0_Apresentacao.pptx) (os slides) e [ROTEIRO_PITCH_TECNICO.md](ROTEIRO_PITCH_TECNICO.md).
>
> **Ponto de partida do relatório:** a demanda oficial do Projeto Integrador 2026/02 cadastrada no SAGA. Cada foco, restrição e benefício esperado da demanda tem lugar definido no texto (ver [A demanda do SAGA](#a-demanda-do-saga)).

## Objetivo

Registrar o projeto em um relatório técnico nas normas da ABNT: o problema, a solução, como foi construída, o que foi entregue e o que fica para depois. O relatório responde, item por item, à demanda do SAGA: mostra como o TechMob 4.0 atende aos quatro focos pedidos, como respeita as quatro restrições e quais benefícios esperados ele concretiza.

A tese é a mesma da apresentação: **o software não é o fim, é a prova** de que dá para aproximar o que se aprende na faculdade do que se aplica na fábrica.

O texto parte do que já está no repositório (plano, READMEs, fotos e código) e não inventa número nem resultado: tudo o que o relatório afirma tem de existir no repositório, na demanda do SAGA ou em uma referência conferida.

## A demanda do SAGA

### Resumo da demanda

| Item | Conteúdo (como está na demanda) |
| :---- | :---- |
| Projeto | Projeto Integrador 2026/02 |
| Título | TechMob 4.0: Soluções Digitais para Indústria 4.0 |
| Programa | TechMob 4.0, "Programe o futuro", iniciativa da Fundação Toyota do Brasil em parceria com o SENAI-SP |
| Curso | Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas |
| Período do programa | Início em janeiro de 2025; conclusão prevista para dezembro de 2026 |
| Objetivo principal | Desenvolver soluções digitais aplicadas a monitoramento, análise de dados e tomada de decisão em ambientes industriais |
| Focos | Monitoramento de processos; análise de dados; apoio à tomada de decisão; aplicação em ambientes produtivos |
| Restrições | Calendário acadêmico; sem acesso a dados industriais confidenciais; segurança da informação, LGPD, privacidade e proteção de dados; tecnologias acessíveis ao ambiente educacional |

### Fio condutor do relatório

A narrativa segue a da apresentação, e cada etapa tem um endereço no texto:

| Etapa | Pergunta que responde | Seções |
| :---- | :---- | :---- |
| Contexto | Por que a Indústria 4.0 e por que o programa TechMob 4.0? | 1.1, 2.1 |
| Problema | O que a demanda pede e qual problema concreto atacamos na bancada? | 1.2 |
| Solução | Como o TechMob 4.0 monitora, analisa e apoia a decisão? | 3 e 4 |
| Conclusão | A demanda foi atendida? Dá para aproximar escola e fábrica? | 5 e 6 |

### Quadro 1: focos da demanda × como o TechMob atende

| Foco da demanda | Como o TechMob 4.0 atende | Onde no relatório | Evidência no repositório |
| :---- | :---- | :---- | :---- |
| Monitoramento de processos | O backend lê o `/dadosPlanta` a cada segundo. O Andon do Dashboard mostra as 3 estações ao vivo e o pedido em andamento contra o tempo padrão | 4.1, 4.6 (Dashboard) | `PlantaClient`, `frontend/README.md`, captura do Dashboard |
| Análise de dados | OEE por pedido (disponibilidade × performance × qualidade), acumulado pela soma dos tempos. Análise da cronometragem em Python. O mesmo OEE conferido em Java e em Python pelas mesmas gravações de teste | 2.2, 4.3, 4.7, 4.8 | `CalculadoraOEE`, `data/README.md`, `backend/src/test/`, `data/tests/` |
| Apoio à tomada de decisão | Comparação com a meta de 75%, alerta de pedido acima do padrão, selo "gargalo" na Produção e detalhe do pedido mostrando em qual etapa o tempo foi perdido | 4.6, 5.1 | Capturas da Produção e do Histórico |
| Aplicação em ambientes produtivos | Construído sobre o contrato real da bancada (CLP Siemens ET 200SP + Node-RED). Para ligar à bancada, basta trocar a `PLANTA_URL`. Limite declarado: bancada didática e simulador da bancada como fonte desde 19/11 | 2.4, 4.2, 5.4, 6 | Fotos de 14/09, `node-red/README.md` |

### Quadro 2: restrições da demanda × como foram respeitadas

| Restrição | Como o projeto respeita | Onde no relatório | Evidência |
| :---- | :---- | :---- | :---- |
| 1. Viável dentro do calendário acadêmico | Trabalho dividido em 14 partes, uma branch e um PR por parte, merge em ordem. Decisão de 19/11 de usar o simulador da bancada como fonte, para fechar dentro do prazo sem depender do acesso ao laboratório | 3, 1.5 | Plano (Ordem de execução, atualização de 19/11) |
| 2. Sem acesso a dados industriais confidenciais | A única fonte é a bancada didática do SENAI e o simulador com os tempos da cronometragem da equipe. Nenhum dado de empresa entra no projeto | 1.5, 3, 4.7 | `data/README.md`, plano |
| 3. Segurança da informação, LGPD, privacidade e proteção de dados | Só leitura na bancada (nada instalado ou alterado). API só com GET. Node-RED acessível só na rede do laboratório. Configuração por variáveis de ambiente, fora do código. Banco com dados de máquina e de pedido, sem dado pessoal (**conferir no `schema.sql` antes de afirmar**). Fotos e vídeos sem dados pessoais | 4.4, 4.5, 5.2 | `infra/schema.sql`, `backend/README.md` |
| 4. Tecnologias acessíveis ao ambiente educacional | Spring Boot, React, MySQL, Python e Node-RED: gratuitos e executados em computador comum do laboratório. Supabase descartado por ser bloqueado na rede do SENAI | 3, 4 | `README.md`, plano |

### Quadro 3: benefícios esperados × evidência no projeto

| Benefício esperado | Evidência no TechMob 4.0 |
| :---- | :---- |
| Preparar os alunos para demandas reais da Indústria 4.0 | Trabalho sobre um CLP industrial (Siemens ET 200SP) e sobre o OEE, indicador padrão da manufatura |
| Integrar conhecimento acadêmico com prática industrial | Visita de 14/09 ao laboratório, cronometragem da bancada e o OEE do TPM aplicado à janela do pedido |
| Desenvolver soluções inovadoras aplicáveis ao setor produtivo | OEE por pedido em vez de por turno; mesmo contrato da bancada, ligado trocando uma URL |
| Estimular a cultura de dados e a transformação digital | Decisão apoiada em número: meta, alerta e gargalo substituem o "achismo" |
| Aumentar a empregabilidade dos participantes | Stack e práticas de mercado: Spring Boot, React, MySQL, Python, Git com PR e revisão, 72 + 18 testes automatizados |
| Fortalecer a integração entre educação e indústria | Projeto dentro do programa da Fundação Toyota do Brasil com o SENAI-SP, apresentado na banca e no SAGA |
| Promover uma formação tecnológica inclusiva | Atendimento indireto: ferramentas gratuitas, documentação em português com passo a passo de execução (Apêndice D) e vídeos com legenda. **Validar com o orientador antes de afirmar** |

## Situação

- **Demanda do SAGA:** mapeada nos Quadros 1 a 3 acima. Falta validar com o orientador.
- **Modelo do SENAI:** ainda não definido. Até ele sair, o relatório segue a NBR 14724 com as regras abaixo. Quando o modelo chegar, muda só a formatação (capa, folha de rosto, margens e fontes); o conteúdo e a estrutura continuam.
- **Conteúdo técnico:** pronto. O código das Partes 1 a 14 está na `main` e os testes passam (72 no backend, 18 no Python).
- **Material visual:** as fotos de 14/09 estão em [`docs/img/`](../img/). As capturas do Dashboard, da Produção e do Histórico também estão lá (`tela-*.png`). Faltam o diagrama exportado, o modelo do banco e a captura do detalhe do pedido (ver Figuras, quadros e tabelas).

## Normas e formatação (até chegar o modelo)

| Item | Regra |
| :---- | :---- |
| Normas | NBR 14724 (trabalhos acadêmicos: estrutura e apresentação), NBR 6023:2018 (referências), NBR 10520:2023 (citações), NBR 6024 (numeração progressiva das seções), NBR 6027 (sumário), NBR 6028 (resumo) e as Normas de Apresentação Tabular do IBGE (tabelas) |
| Papel e margens | A4; superior e esquerda 3 cm; inferior e direita 2 cm |
| Fonte | Uma só família em todo o texto (Arial ou Times New Roman). Tamanho 12 no texto; 10 em citações longas, notas, legendas e fontes de ilustrações e tabelas |
| Espaçamento | 1,5 no texto. Simples em citações longas, notas, referências, legendas e na natureza do trabalho (folha de rosto). Referências separadas entre si por uma linha em branco simples |
| Parágrafo | Recuo de 1,25 cm na primeira linha, texto justificado |
| Citação longa (mais de 3 linhas) | Recuo de 4 cm da margem esquerda, fonte 10, espaçamento simples, sem aspas |
| Citação no sistema autor-data | Pela NBR 10520:2023, o sobrenome vai com inicial maiúscula também entre parênteses: (Nakajima, 1988) |
| Paginação | Contada a partir da folha de rosto e mostrada a partir da Introdução, em algarismos arábicos, no canto superior direito |
| Seções | Numeração progressiva (1, 1.1, 1.1.1) alinhada à esquerda; cada seção primária começa em página nova. Títulos sem número (Resumo, Listas, Sumário, Referências) ficam centralizados |
| Ilustrações | Identificação em cima ("Figura 3 – Arquitetura do TechMob 4.0"), fonte embaixo ("Fonte: elaborado pelos autores (2026)") |
| Tabelas e quadros | Tabela (dados numéricos) segue o IBGE: título em cima, fonte embaixo, sem bordas laterais. Quadro (texto) pode ter bordas. Os Quadros 1 a 3 da demanda levam a fonte "Fonte: elaborado pelos autores com base em SENAI-SP (2026)" |
| Siglas | Na primeira vez, por extenso com a sigla entre parênteses: Overall Equipment Effectiveness (OEE), Lei Geral de Proteção de Dados Pessoais (LGPD) |

## Estrutura do relatório

Título sugerido: **TechMob 4.0: monitoramento do OEE por pedido na Bancada Smart 4.0** (a confirmar com a equipe).

Alternativa que deixa a demanda explícita: **TechMob 4.0: monitoramento, análise de dados e apoio à decisão na Bancada Smart 4.0**.

### Elementos pré-textuais

| Elemento | Obrigatório? | Conteúdo | Responsável |
| :---- | :---- | :---- | :---- |
| Capa | Sim | Instituição (Faculdade de Tecnologia SENAI Antonio Adolpho Lobbe), nomes dos autores, título, local (a confirmar) e ano (2026) | Nykolas |
| Folha de rosto | Sim | Autores, título, natureza do trabalho (Projeto Integrador 2026/02 do Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas, no âmbito do programa TechMob 4.0 da Fundação Toyota do Brasil e do SENAI-SP), orientador (Prof. André Roberto, conferir o nome completo), local e ano | Nykolas |
| Resumo | Sim | 150 a 500 palavras em parágrafo único: demanda (programa e objetivo principal), problema na bancada, objetivo, método, resultados (focos atendidos e restrições respeitadas) e conclusão. 3 a 5 palavras-chave (sugestão: Indústria 4.0; OEE; monitoramento industrial; Bancada Smart 4.0; integração ensino-indústria). Escrito por último | Lucas |
| Abstract | Confirmar | Versão em inglês do resumo, se o modelo exigir | Lucas |
| Lista de ilustrações | Recomendado | Gerada pelo editor a partir das legendas | Nykolas |
| Lista de quadros | Recomendado | Gerada pelo editor (inclui os Quadros 1 a 3 da demanda) | Nykolas |
| Lista de tabelas | Recomendado | Gerada pelo editor | Nykolas |
| Lista de abreviaturas e siglas | Recomendado | ADS, API, CLP, CORS, HTTP, JPA, JSON, LGPD, OEE, REST, SAGA (conferir o nome por extenso), SENAI, SQL, TPM | Nykolas |
| Sumário | Sim | Gerado pelo editor a partir dos títulos (NBR 6027) | Nykolas |

### Elementos textuais

| Seção | O que escrever | Fonte | Responsável |
| :---- | :---- | :---- | :---- |
| 1.1 Contexto | Indústria 4.0 no Brasil (dados da CNI, depois de conferidos) e o programa TechMob 4.0 "Programe o futuro": Fundação Toyota do Brasil e SENAI-SP, curso de ADS, de janeiro de 2025 a dezembro de 2026 | Demanda do SAGA, referências | Lucas |
| 1.2 Demanda e problema | O que a demanda pede (soluções digitais para monitoramento, análise de dados, apoio à decisão e aplicação em ambiente produtivo) e o problema concreto escolhido: na Bancada Smart 4.0, ninguém sabia quanto tempo cada etapa levava nem onde o tempo se perdia. Pano de fundo: a distância entre o que se ensina e o que a indústria aplica | Demanda do SAGA, `README.md` (Objetivo) | Lucas |
| 1.3 Objetivos | **Geral:** desenvolver uma solução digital de monitoramento, análise de dados e apoio à decisão para a Bancada Smart 4.0, com o OEE calculado por pedido. **Específicos (um por foco):** (a) monitorar as 3 estações em tempo real; (b) calcular e guardar o OEE de cada pedido; (c) comparar com a meta, alertar atrasos e apontar o gargalo; (d) operar sobre o contrato real da bancada, de forma que a ligação dependa só da `PLANTA_URL` | Demanda do SAGA, Quadro 1 | Lucas |
| 1.4 Justificativa | Os benefícios esperados da demanda e como o projeto os concretiza (síntese do Quadro 3) | Quadro 3 | Lucas |
| 1.5 Delimitação e restrições | As 4 restrições da demanda e as delimitações do projeto: bancada didática, Node-RED só leitura, simulador da bancada como fonte desde 19/11 | Demanda do SAGA, Quadro 2, plano (atualização de 19/11) | Lucas |
| 2.1 Indústria 4.0 | Conceito e dados do setor (os da CNI usados no deck de 24/10, depois de conferidos) | Referências a levantar | Lucas |
| 2.2 OEE | Disponibilidade, performance e qualidade; origem no TPM; por que a janela do pedido em vez do turno | Plano (Contrato e cálculo), `data/README.md` | Samuel |
| 2.3 Gestão visual e Andon | O que é o Andon e como ele vira o painel das 3 estações (foco "monitoramento") | `frontend/README.md` | Otávio |
| 2.4 CLP, Node-RED e integração | CLP Siemens ET 200SP, o Node-RED da bancada e a leitura por HTTP (por que não MQTT). Por que só leitura atende às restrições 2 e 3 | `node-red/README.md`, fotos | Otávio |
| 2.5 Arquitetura em camadas e API REST | Controller, Service, Repository e DTO; REST só com GET | `backend/README.md` | Rafael |
| 3 Metodologia | Como as restrições guiaram as escolhas: calendário → 14 partes (uma branch e um PR por parte, revisão e merge em ordem) e decisão de 19/11; tecnologias acessíveis → stack gratuita, Supabase descartado. Visita de 14/09 ao laboratório, cronometragem da equipe, ferramentas, estratégia de testes | Plano (Ordem de execução, atualização de 19/11), Quadro 2 | Lucas e Nykolas |
| 4.1 Arquitetura | Diagrama e caminho do dado: fonte → backend (PlantaClient → DetectorPedidos → CalculadoraOEE) → MySQL → React | `README.md` (Arquitetura) | Lucas |
| 4.2 Contrato do `/dadosPlanta` e regras do pedido | Campos do JSON, como um pedido começa, tem etapas, termina, é reprovado ou interrompido | Plano (Contrato), `data/README.md` | Lucas |
| 4.3 Cálculo do OEE por pedido | Fórmulas, exemplo de 80,0%, acumulado pela soma dos tempos (e não pela média dos OEEs) | `README.md` (Como o OEE é calculado) | Samuel |
| 4.4 Banco de dados | 5 tabelas (maquina, pedido, etapa_pedido, indicador_oee, leitura_planta), modelo entidade-relacionamento, seed da máquina. O que o banco **não** guarda: dado pessoal (LGPD, depois de conferido) | `infra/schema.sql` | Rafael |
| 4.5 Backend e API | Pacote coleta, serviços, os 6 endpoints GET, tratamento de erros, configuração por variáveis de ambiente. Subseção curta de segurança: só leitura, só GET, segredos fora do código, CORS | `backend/README.md` | Rafael |
| 4.6 Frontend | Dashboard (Samuel), Produção (Otávio), Histórico e detalhe do pedido (Nykolas): o que cada tela mostra, qual decisão apoia e qual foco da demanda atende | `frontend/README.md`, capturas | Samuel, Otávio e Nykolas |
| 4.7 Python: simulador, coletor e análise | Simulador sintético e replay, coletor, cálculo de conferência e análise da cronometragem. Por que o simulador respeita a restrição 2 (tempos da equipe, nenhum dado de empresa) | `data/README.md` | Samuel |
| 4.8 Testes | 72 testes no backend (10 classes, JUnit) e 18 no Python (pytest); as mesmas gravações de teste conferem o mesmo OEE em Java e em Python | `README.md` (Testes), `backend/src/test/`, `data/tests/` | Rafael e Samuel |
| 5.1 Atendimento aos focos da demanda | Telas funcionando e um exemplo de pedido, organizados pelos 4 focos (Quadro 1) | Seções 4.1 a 4.7 | Lucas (consolidação) |
| 5.2 Atendimento às restrições | Como cada restrição foi respeitada (Quadro 2) | Seções 3, 4.4 e 4.5 | Lucas e Rafael |
| 5.3 Benefícios esperados | O que o projeto concretizou de cada benefício (Quadro 3), separando o que é direto do que é indireto | Quadro 3 | Lucas |
| 5.4 Limitações | Sem gravação real da bancada; textos de status além de AGUARDANDO vindos do simulador; qualidade só por pedido; aplicação em ambiente produtivo demonstrada na bancada didática, não em linha real | Plano (Riscos e pendências) | Todos; consolidação: Lucas |
| 6 Considerações finais | Resposta direta ao objetivo principal da demanda: solução digital de monitoramento, análise e decisão entregue, dentro das restrições. A tese: o TechMob 4.0 é a prova de que dá para aproximar escola e fábrica. Trabalhos futuros: ligar à bancada trocando a `PLANTA_URL`, gravar execuções reais, qualidade por peça | Demanda do SAGA, plano | Lucas |

### Elementos pós-textuais

| Elemento | Conteúdo | Responsável |
| :---- | :---- | :---- |
| Referências | Só o que foi citado no texto, em ordem alfabética, pela NBR 6023:2018 | Nykolas |
| Apêndice A | Contrato da API: exemplos de JSON de cada endpoint | Rafael |
| Apêndice B | Script do banco (resumo do `schema.sql`) ou o modelo entidade-relacionamento em tamanho maior | Rafael |
| Apêndice C | Fotos do laboratório de 14/09 (feitas pela equipe, por isso apêndice e não anexo) | Otávio |
| Apêndice D | Como executar o sistema (os 4 passos do README) | Nykolas |
| Anexo A | Texto da demanda do Projeto Integrador 2026/02 cadastrada no SAGA (não foi produzido pela equipe, por isso anexo). Entra só com o aval do orientador | Lucas |

## Figuras, quadros e tabelas planejados

| Item | Conteúdo | Origem | Quem produz |
| :---- | :---- | :---- | :---- |
| Figura | Bancada Smart 4.0 (frente e estações) | `docs/img/bancada-frente.jpg`, `docs/img/bancada-estacoes.jpg` | Pronto |
| Figura | Painel elétrico e CLP ET 200SP | `docs/img/painel-et200sp.jpg`, `docs/img/tia-portal-et200sp.jpg` | Pronto |
| Figura | Flow do Node-RED e o JSON no navegador | `docs/img/node-red-api-dados-planta.jpg`, `docs/img/json-dadosplanta-navegador.jpg` | Pronto |
| Figura | Arquitetura do TechMob 4.0 | Exportar o diagrama mermaid do README como PNG ou SVG (mermaid.live) | Lucas |
| Figura | Modelo entidade-relacionamento | MySQL Workbench (Database → Reverse Engineer) | Rafael |
| Figura | Dashboard, Produção e Histórico | `docs/img/tela-dashboard.png`, `docs/img/tela-producao.png`, `docs/img/tela-historico.png` | Pronto |
| Figura | Detalhe do pedido (linha do tempo das etapas) | Captura do Histórico com o botão › aberto | Nykolas |
| Quadro | Focos da demanda × como o TechMob atende (Quadro 1) | Esta página | Lucas |
| Quadro | Restrições da demanda × como foram respeitadas (Quadro 2) | Esta página | Lucas; linha da LGPD conferida por Rafael |
| Quadro | Benefícios esperados × evidência (Quadro 3) | Esta página | Lucas |
| Quadro | Campos do JSON do `/dadosPlanta` e uso | Plano (Contrato do dado) | Lucas |
| Quadro | Endpoints da API | `backend/README.md` | Rafael |
| Tabela | Tempo padrão por etapa (20 s, 35 s, 25 s) e meta de OEE (75%) | `README.md` (Valores do projeto) | Samuel |
| Tabela | Testes por parte (backend 72, Python 18) | `README.md` (Testes) | Rafael |

## Referências a levantar

Regra da equipe: nenhuma referência entra sem ter sido lida e conferida (autor, título, edição, editora, ano e páginas). Sites entram com "Disponível em" e "Acesso em".

| Referência | Para quê | Situação |
| :---- | :---- | :---- |
| SENAI-SP. Projeto Integrador 2026/02: TechMob 4.0: Soluções Digitais para Indústria 4.0. Demanda cadastrada no SAGA | Seções 1, 5 e 6; fonte dos Quadros 1 a 3 | Temos o texto. Falta confirmar com o orientador o autor institucional, a data e se pode ser citado e anexado |
| BRASIL. Lei nº 13.709, de 14 de agosto de 2018. Lei Geral de Proteção de Dados Pessoais (LGPD) | Seções 1.5 e 5.2 | Citar pelo site do Planalto, com data de acesso |
| NAKAJIMA, Seiichi. Livro sobre TPM, origem do OEE (edição original em inglês de 1988; há tradução brasileira) | Seção 2.2 | Conferir qual edição a equipe vai usar e os dados completos |
| CNI. Sondagem Especial Indústria 4.0 (2021), fonte do dado de 69% usado no deck de 24/10 | Seção 1.1 e 2.1 | Conferir na publicação original |
| Fonte do dado de 60% (falta de mão de obra qualificada), citado no deck como "CNI via Band Jornalismo, fev/2026" | Seção 1.1 e 2.1 | Buscar a publicação original da CNI; se não achar, não usar |
| Material da Fundação Toyota do Brasil sobre o programa TechMob 4.0 | Seção 1.1 | Pedir ao orientador; se houver página oficial, citar com data de acesso |
| Documentação oficial do Spring Boot, do React, do Node-RED e do MySQL | Seções 2.4, 2.5 e 4 | Citar como documento online, com data de acesso |
| Material da Siemens sobre o ET 200SP | Seção 2.4 | Opcional; conferir |

## Responsáveis

| Integrante | Seções |
| :---- | :---- |
| Lucas | 1.1 a 1.5, 2.1, 3 (com Nykolas), 4.1, 4.2, Quadros 1 a 3 da demanda, 5 (consolidação), 6, resumo, abstract e Anexo A |
| Rafael | 2.5, 4.4 (com a conferência de dado pessoal), 4.5 (com a subseção de segurança), 4.8 (com Samuel), 5.2 (com Lucas), Apêndices A e B, modelo entidade-relacionamento |
| Samuel | 2.2, 4.3, 4.6 (Dashboard), 4.7, 4.8 (com Rafael) |
| Otávio | 2.3, 2.4, 4.6 (Produção), Apêndice C |
| Nykolas | Pré-textuais, 3 (com Lucas), 4.6 (Histórico), referências, Apêndice D, formatação ABNT final e revisão de todo o texto |

Revisão cruzada: cada seção é lida por alguém de outra parte antes da formatação (mesma regra dos PRs). Quem revisa confere também se a seção cita o foco ou a restrição da demanda que ela atende.

## Cronograma

B é a data de entrega do relatório (se não houver uma data própria, a data da banca). Se B estiver a menos de 3 semanas, as etapas se comprimem, mas a ordem fica.

| Prazo | Etapa | Responsável |
| :---- | :---- | :---- |
| B−21 | Título, estrutura e responsáveis confirmados; Quadros 1 a 3 da demanda validados com o orientador; documento compartilhado criado com as seções vazias | Lucas e Nykolas |
| B−18 | Referências levantadas e conferidas (incluindo a demanda e a LGPD) | Todos; consolidação: Nykolas |
| B−14 | Rascunho de todas as seções textuais | Cada responsável |
| B−10 | Revisão cruzada concluída; figuras e capturas prontas | Todos |
| B−7 | Resumo, resultados por foco e restrição, considerações finais e formatação ABNT completa | Lucas e Nykolas |
| B−3 | Versão final em PDF, conferida com o checklist | Nykolas |
| B | Entrega | Lucas |

## Checklist final

- [ ] Capa e folha de rosto conferidas com o modelo do SENAI (ou com a NBR 14724, se o modelo não sair)
- [ ] Folha de rosto cita o Projeto Integrador 2026/02 e o programa TechMob 4.0 (Fundação Toyota do Brasil e SENAI-SP)
- [ ] Resumo com 150 a 500 palavras, citando a demanda, e 3 a 5 palavras-chave
- [ ] Introdução apresenta a demanda com programa, parceria, curso e período corretos
- [ ] Os 4 focos, as 4 restrições e os 7 benefícios aparecem nos Quadros 1 a 3 e são retomados em 5.1, 5.2 e 5.3
- [ ] Nada afirmado sobre LGPD e segurança que não esteja no código (`schema.sql`, variáveis de ambiente, endpoints GET)
- [ ] Benefícios indiretos (como formação inclusiva) escritos como indiretos, sem exagero
- [ ] Considerações finais respondem ao objetivo principal da demanda
- [ ] Sumário gerado automaticamente e com as páginas certas
- [ ] Seções numeradas, primárias em página nova
- [ ] Toda figura, quadro e tabela com identificação em cima, fonte embaixo e citação no texto
- [ ] Toda citação no texto tem referência, e toda referência foi citada
- [ ] Siglas por extenso na primeira vez (incluindo LGPD e SAGA)
- [ ] Números iguais aos do repositório: 80 s por pedido (20/35/25), meta de 75%, 72 + 18 testes, 6 endpoints, 5 tabelas
- [ ] Linguagem da decisão de 19/11: "contrato real da bancada" e "simulador da bancada", nunca "dados reais da bancada"
- [ ] Margens, fonte, espaçamento e paginação conferidos
- [ ] PDF final gerado e aberto em outro computador

## Pendências

- Validar os Quadros 1 a 3 da demanda com o orientador.
- Referência formal da demanda do SAGA (autor institucional, data) e se ela pode ir como Anexo A.
- Nome por extenso da sigla SAGA.
- Conferir no `schema.sql` e nos logs do backend que nenhuma tabela ou log guarda dado pessoal.
- Salvar o texto da demanda no repositório, para servir de fonte (sugestão: `docs/demanda-saga.txt`).
- Modelo do relatório do SENAI.
- Abstract obrigatório ou não.
- Número de páginas esperado (mínimo ou máximo).
- Formato e local de entrega (PDF, impresso, plataforma).
- Data de entrega do relatório e data da banca.
- Nome completo do orientador e cidade da capa.

# Relatório técnico (ABNT): plano de ação

Atualizado em 20/11/2026 · Equipe TechMob 4.0

> Plano de ação da equipe para o relatório técnico do TechMob 4.0. Complementa o [Plano de alinhamento](../PLANO_ALINHAMENTO.md), que é a fonte das decisões, do contrato e da arquitetura. Os outros entregáveis estão em [APRESENTACAO_SLIDES.md](APRESENTACAO_SLIDES.md) e [ROTEIRO_PITCH_TECNICO.md](ROTEIRO_PITCH_TECNICO.md).

## Objetivo

Registrar o projeto em um relatório técnico nas normas da ABNT: o problema, a solução, como foi construída, o que foi entregue e o que fica para depois. O texto parte do que já está no repositório (plano, READMEs, fotos e código) e não inventa número nem resultado: tudo o que o relatório afirma tem de existir no repositório ou em uma referência conferida.

## Situação

- **Modelo do SENAI:** ainda não definido. Até ele sair, o relatório segue a NBR 14724 com as regras abaixo. Quando o modelo chegar, muda só a formatação (capa, folha de rosto, margens e fontes); o conteúdo e a estrutura continuam.
- **Conteúdo técnico:** pronto. O código das Partes 1 a 14 está na `main` e os testes passam (72 no backend, 18 no Python).
- **Material visual:** as fotos de 14/09 estão em [`docs/img/`](../img/). Faltam as capturas das telas, o diagrama exportado e o modelo do banco (ver Figuras e tabelas).

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
| Tabelas e quadros | Tabela (dados numéricos) segue o IBGE: título em cima, fonte embaixo, sem bordas laterais. Quadro (texto) pode ter bordas |
| Siglas | Na primeira vez, por extenso com a sigla entre parênteses: Overall Equipment Effectiveness (OEE) |

## Estrutura do relatório

Título sugerido: **TechMob 4.0: monitoramento do OEE por pedido na Bancada Smart 4.0** (a confirmar com a equipe).

### Elementos pré-textuais

| Elemento | Obrigatório? | Conteúdo | Responsável |
| :---- | :---- | :---- | :---- |
| Capa | Sim | Instituição (Faculdade de Tecnologia SENAI Antonio Adolpho Lobbe), nomes dos autores, título, local (a confirmar) e ano (2026) | Nykolas |
| Folha de rosto | Sim | Autores, título, natureza do trabalho (Projeto Integrador do curso de Análise e Desenvolvimento de Sistemas), orientador (Prof. André Roberto, conferir o nome completo), local e ano | Nykolas |
| Resumo | Sim | 150 a 500 palavras em parágrafo único: problema, objetivo, método, resultados e conclusão. 3 a 5 palavras-chave (sugestão: Indústria 4.0; OEE; monitoramento industrial; Bancada Smart 4.0; Spring Boot). Escrito por último | Lucas |
| Abstract | Confirmar | Versão em inglês do resumo, se o modelo exigir | Lucas |
| Lista de ilustrações | Recomendado | Gerada pelo editor a partir das legendas | Nykolas |
| Lista de tabelas | Recomendado | Gerada pelo editor | Nykolas |
| Lista de abreviaturas e siglas | Recomendado | ADS, API, CLP, CORS, HTTP, JPA, JSON, OEE, REST, SAGA, SENAI, SQL, TPM | Nykolas |
| Sumário | Sim | Gerado pelo editor a partir dos títulos (NBR 6027) | Nykolas |

### Elementos textuais

| Seção | O que escrever | Fonte no repositório | Responsável |
| :---- | :---- | :---- | :---- |
| 1 Introdução | Contexto (Indústria 4.0 no Brasil), problema (demanda da Fundação Toyota do Brasil e do SENAI-SP: aproximar a indústria da academia), objetivo geral, objetivos específicos, justificativa e delimitação (bancada didática, Node-RED só leitura, simulador como fonte desde 19/11) | `README.md` (Objetivo), plano (Resumo e atualização de 19/11) | Lucas |
| 2.1 Indústria 4.0 | Conceito e dados do setor (os da CNI usados no deck de 24/10, depois de conferidos) | Referências a levantar | Lucas |
| 2.2 OEE | Disponibilidade, performance e qualidade; origem no TPM; por que a janela do pedido em vez do turno | Plano (Contrato e cálculo), `data/README.md` | Samuel |
| 2.3 Gestão visual e Andon | O que é o Andon e como ele vira o painel das 3 estações | `frontend/README.md` | Otávio |
| 2.4 CLP, Node-RED e integração | CLP Siemens ET 200SP, o Node-RED da bancada e a leitura por HTTP (por que não MQTT) | `node-red/README.md`, fotos | Otávio |
| 2.5 Arquitetura em camadas e API REST | Controller, Service, Repository e DTO; REST só com GET | `backend/README.md` | Rafael |
| 3 Metodologia | Organização em 14 partes (uma branch e um PR por parte, revisão e merge em ordem), visita de 14/09 ao laboratório, cronometragem da equipe, decisão de 19/11, ferramentas, estratégia de testes | Plano (Ordem de execução, atualização de 19/11) | Lucas e Nykolas |
| 4.1 Arquitetura | Diagrama e caminho do dado: fonte → backend (PlantaClient → DetectorPedidos → CalculadoraOEE) → MySQL → React | `README.md` (Arquitetura) | Lucas |
| 4.2 Contrato do `/dadosPlanta` e regras do pedido | Campos do JSON, como um pedido começa, tem etapas, termina, é reprovado ou interrompido | Plano (Contrato), `data/README.md` | Lucas |
| 4.3 Cálculo do OEE por pedido | Fórmulas, exemplo de 80,0%, acumulado pela soma dos tempos (e não pela média dos OEEs) | `README.md` (Como o OEE é calculado) | Samuel |
| 4.4 Banco de dados | 5 tabelas (maquina, pedido, etapa_pedido, indicador_oee, leitura_planta), modelo entidade-relacionamento, seed da máquina | `infra/schema.sql` | Rafael |
| 4.5 Backend e API | Pacote coleta, serviços, os 6 endpoints GET, tratamento de erros, configuração por variáveis de ambiente | `backend/README.md` | Rafael |
| 4.6 Frontend | Dashboard (Samuel), Produção (Otávio), Histórico e detalhe do pedido (Nykolas): o que cada tela mostra e qual decisão apoia | `frontend/README.md`, capturas | Samuel, Otávio e Nykolas |
| 4.7 Python: simulador, coletor e análise | Simulador sintético e replay, coletor, cálculo de conferência e análise da cronometragem | `data/README.md` | Samuel |
| 4.8 Testes | 72 testes no backend (10 classes, JUnit) e 18 no Python (pytest); as mesmas gravações de teste conferem o mesmo OEE em Java e em Python | `README.md` (Testes), `backend/src/test/`, `data/tests/` | Rafael e Samuel |
| 5 Resultados e discussão | Telas funcionando, exemplo de pedido, atendimento aos eixos da demanda SAGA (tabela do README), limitações: sem gravação real da bancada, textos de status além de AGUARDANDO vindos do simulador, qualidade só por pedido | Todas as anteriores | Todos; consolidação: Lucas |
| 6 Considerações finais | Objetivos atendidos e trabalhos futuros: ligar à bancada trocando a `PLANTA_URL`, gravar execuções reais, qualidade por peça | Plano (Riscos e pendências) | Lucas |

### Elementos pós-textuais

| Elemento | Conteúdo | Responsável |
| :---- | :---- | :---- |
| Referências | Só o que foi citado no texto, em ordem alfabética, pela NBR 6023:2018 | Nykolas |
| Apêndice A | Contrato da API: exemplos de JSON de cada endpoint | Rafael |
| Apêndice B | Script do banco (resumo do `schema.sql`) ou o modelo entidade-relacionamento em tamanho maior | Rafael |
| Apêndice C | Fotos do laboratório de 14/09 (feitas pela equipe, por isso apêndice e não anexo) | Otávio |
| Apêndice D | Como executar o sistema (os 4 passos do README) | Nykolas |

Anexo só para material que não foi produzido pela equipe (por exemplo, trecho de manual da Siemens). Se não houver, o relatório não tem anexos.

## Figuras e tabelas planejadas

| Item | Conteúdo | Origem | Quem produz |
| :---- | :---- | :---- | :---- |
| Figura | Bancada Smart 4.0 (frente e estações) | `docs/img/bancada-frente.jpg`, `docs/img/bancada-estacoes.jpg` | Pronto |
| Figura | Painel elétrico e CLP ET 200SP | `docs/img/painel-et200sp.jpg`, `docs/img/tia-portal-et200sp.jpg` | Pronto |
| Figura | Flow do Node-RED e o JSON no navegador | `docs/img/node-red-api-dados-planta.jpg`, `docs/img/json-dadosplanta-navegador.jpg` | Pronto |
| Figura | Arquitetura do TechMob 4.0 | Exportar o diagrama mermaid do README como PNG ou SVG (mermaid.live) | Lucas |
| Figura | Modelo entidade-relacionamento | MySQL Workbench (Database → Reverse Engineer) | Rafael |
| Figura | Dashboard, Produção, Histórico e detalhe do pedido | Capturas do ensaio completo, depois de 30 min de simulador rodando | Samuel, Otávio e Nykolas |
| Quadro | Eixos da demanda SAGA × como o TechMob atende | `README.md` (Objetivo) | Lucas |
| Quadro | Campos do JSON do `/dadosPlanta` e uso | Plano (Contrato do dado) | Lucas |
| Quadro | Endpoints da API | `backend/README.md` | Rafael |
| Tabela | Tempo padrão por etapa (20 s, 35 s, 25 s) e meta de OEE (75%) | `README.md` (Valores do projeto) | Samuel |
| Tabela | Testes por parte (backend 72, Python 18) | `README.md` (Testes) | Rafael |

## Referências a levantar

Regra da equipe: nenhuma referência entra sem ter sido lida e conferida (autor, título, edição, editora, ano e páginas). Sites entram com "Disponível em" e "Acesso em".

| Referência | Para quê | Situação |
| :---- | :---- | :---- |
| NAKAJIMA, Seiichi. Livro sobre TPM, origem do OEE (edição original em inglês de 1988; há tradução brasileira) | Seção 2.2 | Conferir qual edição a equipe vai usar e os dados completos |
| CNI. Sondagem Especial Indústria 4.0 (2021), fonte do dado de 69% usado no deck de 24/10 | Seção 2.1 | Conferir na publicação original |
| Fonte do dado de 60% (falta de mão de obra qualificada), citado no deck como "CNI via Band Jornalismo, fev/2026" | Seção 2.1 | Buscar a publicação original da CNI; se não achar, não usar |
| Documentação oficial do Spring Boot, do React, do Node-RED e do MySQL | Seções 2.4, 2.5 e 4 | Citar como documento online, com data de acesso |
| Material da Siemens sobre o ET 200SP | Seção 2.4 | Opcional; conferir |
| Material do SENAI ou da Fundação Toyota sobre a demanda do Projeto Integrador | Seção 1 | Pedir ao orientador |

## Responsáveis

| Integrante | Seções |
| :---- | :---- |
| Lucas | 1, 2.1, 3 (com Nykolas), 4.1, 4.2, 5 (consolidação), 6, resumo e abstract |
| Rafael | 2.5, 4.4, 4.5, 4.8 (com Samuel), Apêndices A e B, modelo entidade-relacionamento |
| Samuel | 2.2, 4.3, 4.6 (Dashboard), 4.7, 4.8 (com Rafael) |
| Otávio | 2.3, 2.4, 4.6 (Produção), Apêndice C |
| Nykolas | Pré-textuais, 3 (com Lucas), 4.6 (Histórico), referências, Apêndice D, formatação ABNT final e revisão de todo o texto |

Revisão cruzada: cada seção é lida por alguém de outra parte antes da formatação (mesma regra dos PRs).

## Cronograma

B é a data de entrega do relatório (se não houver uma data própria, a data da banca). Se B estiver a menos de 3 semanas, as etapas se comprimem, mas a ordem fica.

| Prazo | Etapa | Responsável |
| :---- | :---- | :---- |
| B−21 | Título, estrutura e responsáveis confirmados; documento compartilhado criado com as seções vazias | Lucas e Nykolas |
| B−18 | Referências levantadas e conferidas | Todos; consolidação: Nykolas |
| B−14 | Rascunho de todas as seções textuais | Cada responsável |
| B−10 | Revisão cruzada concluída; figuras e capturas prontas | Todos |
| B−7 | Resumo, considerações finais e formatação ABNT completa | Lucas e Nykolas |
| B−3 | Versão final em PDF, conferida com o checklist | Nykolas |
| B | Entrega | Lucas |

## Checklist final

- [ ] Capa e folha de rosto conferidas com o modelo do SENAI (ou com a NBR 14724, se o modelo não sair)
- [ ] Resumo com 150 a 500 palavras e 3 a 5 palavras-chave
- [ ] Sumário gerado automaticamente e com as páginas certas
- [ ] Seções numeradas, primárias em página nova
- [ ] Toda figura, quadro e tabela com identificação em cima, fonte embaixo e citação no texto
- [ ] Toda citação no texto tem referência, e toda referência foi citada
- [ ] Siglas por extenso na primeira vez
- [ ] Números iguais aos do repositório: 80 s por pedido (20/35/25), meta de 75%, 72 + 18 testes, 6 endpoints, 5 tabelas
- [ ] Linguagem da decisão de 19/11: "contrato real da bancada" e "simulador da bancada", nunca "dados reais da bancada"
- [ ] Margens, fonte, espaçamento e paginação conferidos
- [ ] PDF final gerado e aberto em outro computador

## Pendências

- Modelo do relatório do SENAI.
- Abstract obrigatório ou não.
- Número de páginas esperado (mínimo ou máximo).
- Formato e local de entrega (PDF, impresso, plataforma).
- Data de entrega do relatório e data da banca.
- Nome completo do orientador e cidade da capa.

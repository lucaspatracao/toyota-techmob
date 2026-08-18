-- =====================================================================
-- TechMob 4.0 - Sistema de Monitoramento de Eficiência Fabril (OEE)
-- Schema inicial do PostgreSQL (dados estruturais)
--
-- Observação: os dados BRUTOS de produção (ciclo a ciclo) ficam em CSV,
-- conforme decisão do documento do projeto. Este schema cobre apenas os
-- dados estruturais: máquinas, indicadores calculados e histórico
-- consolidado que o back-end expõe via API.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS techmob;
SET search_path TO techmob;

-- ---------------------------------------------------------------------
-- Tabela: maquina
-- Cadastro das máquinas/bancadas monitoradas.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS maquina (
    id                          BIGSERIAL PRIMARY KEY,
    identificador_bancada       VARCHAR(50)  NOT NULL UNIQUE, -- ex: BANCADA_SMART_01
    nome                        VARCHAR(120) NOT NULL,
    status_operacional          VARCHAR(30)  NOT NULL DEFAULT 'DESCONHECIDO', -- EM_PRODUCAO, PARADA, MANUTENCAO...
    capacidade_teorica          NUMERIC(10,2) NOT NULL, -- peças/segundo (ou unidade de tempo definida)
    tempo_planejado_producao_s  NUMERIC(12,2) NOT NULL, -- tempo planejado de produção, em segundos
    criado_em                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Tabela: indicador_oee
-- Indicadores de OEE já calculados pelo módulo Python, por período.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS indicador_oee (
    id                  BIGSERIAL PRIMARY KEY,
    maquina_id          BIGINT NOT NULL REFERENCES maquina(id) ON DELETE CASCADE,
    periodo_inicio      TIMESTAMPTZ NOT NULL,
    periodo_fim         TIMESTAMPTZ NOT NULL,
    disponibilidade     NUMERIC(6,3) NOT NULL, -- percentual (0-100)
    performance         NUMERIC(6,3) NOT NULL, -- percentual (0-100)
    qualidade           NUMERIC(6,3) NOT NULL, -- percentual (0-100)
    oee                 NUMERIC(6,3) NOT NULL, -- percentual consolidado (0-100)
    pecas_boas          INTEGER NOT NULL DEFAULT 0,
    pecas_defeituosas   INTEGER NOT NULL DEFAULT 0,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_periodo CHECK (periodo_fim > periodo_inicio)
);

CREATE INDEX IF NOT EXISTS idx_indicador_oee_maquina_periodo
    ON indicador_oee (maquina_id, periodo_inicio DESC);

-- ---------------------------------------------------------------------
-- Tabela: historico_producao
-- Histórico consolidado (não é o CSV bruto!) usado para tabelas/listagens
-- no dashboard sem precisar reprocessar os arquivos CSV a cada request.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS historico_producao (
    id                      BIGSERIAL PRIMARY KEY,
    maquina_id              BIGINT NOT NULL REFERENCES maquina(id) ON DELETE CASCADE,
    "timestamp"             TIMESTAMPTZ NOT NULL,
    status_operacional      VARCHAR(30) NOT NULL,
    pecas_boas              INTEGER NOT NULL DEFAULT 0,
    pecas_defeituosas       INTEGER NOT NULL DEFAULT 0,
    tempo_ciclo_segundos    NUMERIC(10,3),
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_historico_producao_maquina_timestamp
    ON historico_producao (maquina_id, "timestamp" DESC);

-- ---------------------------------------------------------------------
-- Seed opcional: uma máquina de exemplo para facilitar os testes iniciais
-- do backend e do front-end antes da bancada real estar integrada.
-- ---------------------------------------------------------------------
INSERT INTO maquina (identificador_bancada, nome, status_operacional, capacidade_teorica, tempo_planejado_producao_s)
VALUES ('BANCADA_SMART_01', 'Bancada Smart 4.0 - Lab SENAI', 'PARADA', 10.0, 28800)
ON CONFLICT (identificador_bancada) DO NOTHING;

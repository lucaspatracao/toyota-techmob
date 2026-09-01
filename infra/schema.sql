-- =====================================================================
-- TechMob 4.0 - Sistema de Monitoramento de Eficiência Fabril (OEE)
-- Schema inicial do MySQL (dados estruturais)
--
-- Observação: os dados BRUTOS de produção (ciclo a ciclo) ficam em CSV,
-- conforme decisão do documento do projeto. Este schema cobre apenas os
-- dados estruturais: máquinas, indicadores calculados e histórico
-- consolidado que o back-end expõe via API.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS techmob CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE techmob;

-- ---------------------------------------------------------------------
-- Tabela: maquina
-- Cadastro das máquinas/bancadas monitoradas.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS maquina (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    identificador_bancada       VARCHAR(50) NOT NULL UNIQUE, -- ex: BANCADA_SMART_01
    nome                        VARCHAR(120) NOT NULL,
    status_operacional          VARCHAR(30) NOT NULL DEFAULT 'DESCONHECIDO', -- EM_PRODUCAO, PARADA, MANUTENCAO...
    capacidade_teorica          DECIMAL(10,2) NOT NULL, -- peças/segundo (ou unidade de tempo definida)
    tempo_planejado_producao_s  DECIMAL(12,2) NOT NULL, -- tempo planejado de produção, em segundos
    criado_em                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- Tabela: indicador_oee
-- Indicadores de OEE já calculados pelo módulo Python, por período.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS indicador_oee (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    maquina_id          BIGINT NOT NULL,
    periodo_inicio      TIMESTAMP NOT NULL,
    periodo_fim         TIMESTAMP NOT NULL,
    disponibilidade     DECIMAL(6,3) NOT NULL, -- percentual (0-100)
    performance         DECIMAL(6,3) NOT NULL, -- percentual (0-100)
    qualidade           DECIMAL(6,3) NOT NULL, -- percentual (0-100)
    oee                 DECIMAL(6,3) NOT NULL, -- percentual consolidado (0-100)
    pecas_boas          INT NOT NULL DEFAULT 0,
    pecas_defeituosas   INT NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_indicador_oee_maquina FOREIGN KEY (maquina_id) REFERENCES maquina(id) ON DELETE CASCADE,
    CONSTRAINT chk_periodo CHECK (periodo_fim > periodo_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_indicador_oee_maquina_periodo
    ON indicador_oee (maquina_id, periodo_inicio DESC);

-- ---------------------------------------------------------------------
-- Tabela: historico_producao
-- Histórico consolidado (não é o CSV bruto!) usado para tabelas/listagens
-- no dashboard sem precisar reprocessar os arquivos CSV a cada request.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS historico_producao (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    maquina_id              BIGINT NOT NULL,
    `timestamp`             TIMESTAMP NOT NULL,
    status_operacional      VARCHAR(30) NOT NULL,
    pecas_boas              INT NOT NULL DEFAULT 0,
    pecas_defeituosas       INT NOT NULL DEFAULT 0,
    tempo_ciclo_segundos    DECIMAL(10,3),
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historico_producao_maquina FOREIGN KEY (maquina_id) REFERENCES maquina(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_historico_producao_maquina_timestamp
    ON historico_producao (maquina_id, `timestamp` DESC);

-- ---------------------------------------------------------------------
-- Seed opcional: uma máquina de exemplo para facilitar os testes iniciais
-- do backend e do front-end antes da bancada real estar integrada.
-- ---------------------------------------------------------------------
INSERT INTO maquina (identificador_bancada, nome, status_operacional, capacidade_teorica, tempo_planejado_producao_s)
VALUES ('BANCADA_SMART_01', 'Bancada Smart 4.0 - Lab SENAI', 'PARADA', 10.0, 28800)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    status_operacional = VALUES(status_operacional),
    capacidade_teorica = VALUES(capacidade_teorica),
    tempo_planejado_producao_s = VALUES(tempo_planejado_producao_s);

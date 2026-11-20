-- =====================================================================
-- TechMob 4.0 - Sistema de Monitoramento de Eficiência Fabril (OEE)
-- Schema do MySQL
--
-- O dado bruto vem do GET /dadosPlanta do Node-RED da Bancada Smart 4.0
-- (somente leitura). O backend consulta a bancada a cada 1 s, identifica
-- cada pedido de 3 peças pelas mudanças de estado e grava aqui o pedido,
-- as etapas e o OEE calculado. O OEE é por pedido, não por turno.
--
-- Atualização: o banco é local e ainda não tem dado real, então a troca
-- de versão é recriar tudo:
--     DROP DATABASE techmob;
-- e rodar este arquivo de novo.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS techmob CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE techmob;

-- ---------------------------------------------------------------------
-- Tabela: maquina
-- Cadastro das bancadas monitoradas.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS maquina (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    identificador_bancada   VARCHAR(50) NOT NULL UNIQUE, -- ex: BANCADA_SMART_01
    nome                    VARCHAR(120) NOT NULL,
    status_operacional      VARCHAR(30) NOT NULL DEFAULT 'SEM_CONEXAO', -- AGUARDANDO, PRODUZINDO, PARADO, SEM_CONEXAO
    tempo_padrao_pedido_s   DECIMAL(10,3) NOT NULL, -- soma da cronometragem das etapas de um pedido, em segundos
    pecas_por_pedido        INT NOT NULL DEFAULT 3,
    ultima_leitura_em       TIMESTAMP(3) NULL DEFAULT NULL, -- última leitura bem-sucedida do /dadosPlanta
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- Tabela: pedido
-- Um pedido da bancada (3 peças), do início (aguardando 1 -> 0) ao fim
-- (aguardando volta para 1). Substitui a antiga historico_producao.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pedido (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    maquina_id          BIGINT NOT NULL,
    numero              INT NOT NULL,                -- sequência do pedido na máquina (1, 2, 3...)
    origem              VARCHAR(20) NOT NULL,        -- BANCADA, REPLAY, SINTETICO
    situacao            VARCHAR(20) NOT NULL,        -- EM_ANDAMENTO, CONCLUIDO, INTERROMPIDO
    inicio              TIMESTAMP(3) NOT NULL,
    fim                 TIMESTAMP(3) NULL DEFAULT NULL,
    lead_time_s         DECIMAL(10,3) NULL,          -- do início ao fim, em segundos
    tempo_parado_s      DECIMAL(10,3) NOT NULL DEFAULT 0,
    reprovado           BOOLEAN NOT NULL DEFAULT FALSE,
    pecas_boas          INT NOT NULL DEFAULT 0,
    pecas_defeituosas   INT NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pedido_maquina FOREIGN KEY (maquina_id) REFERENCES maquina(id) ON DELETE CASCADE,
    CONSTRAINT uk_pedido_maquina_numero UNIQUE (maquina_id, numero),
    CONSTRAINT chk_pedido_periodo CHECK (fim >= inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pedido_maquina_inicio
    ON pedido (maquina_id, inicio DESC);

-- ---------------------------------------------------------------------
-- Tabela: etapa_pedido
-- Tempo de cada estação dentro de um pedido. A etapa abre quando a
-- estação sai de AGUARDANDO e fecha quando volta. tempo_padrao_s vem da
-- cronometragem da equipe e fica gravado junto, para o histórico não
-- mudar quando o padrão for atualizado.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS etapa_pedido (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id           BIGINT NOT NULL,
    estacao             VARCHAR(20) NOT NULL,        -- ESTOQUE, MANIPULACAO, SEPARACAO
    inicio              TIMESTAMP(3) NOT NULL,
    fim                 TIMESTAMP(3) NOT NULL,
    duracao_s           DECIMAL(10,3) NOT NULL,      -- tempo com a estação fora de AGUARDANDO
    tempo_parado_s      DECIMAL(10,3) NOT NULL DEFAULT 0,
    tempo_padrao_s      DECIMAL(10,3) NOT NULL,

    CONSTRAINT fk_etapa_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
    CONSTRAINT chk_etapa_periodo CHECK (fim >= inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_etapa_pedido_pedido
    ON etapa_pedido (pedido_id);

-- ---------------------------------------------------------------------
-- Tabela: indicador_oee
-- OEE de um pedido concluído (um indicador por pedido), calculado pelo
-- backend: D = (L - T_parado) / L, P = mín(1; T_padrão / (L - T_parado)),
-- Q = 1 (aprovado) ou 0 (reprovado), OEE = D x P x Q. Percentuais 0-100.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS indicador_oee (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    maquina_id          BIGINT NOT NULL,
    pedido_id           BIGINT NOT NULL UNIQUE,
    periodo_inicio      TIMESTAMP(3) NOT NULL,       -- início do pedido
    periodo_fim         TIMESTAMP(3) NOT NULL,       -- fim do pedido
    disponibilidade     DECIMAL(6,3) NOT NULL,
    performance         DECIMAL(6,3) NOT NULL,
    qualidade           DECIMAL(6,3) NOT NULL,
    oee                 DECIMAL(6,3) NOT NULL,
    pecas_boas          INT NOT NULL DEFAULT 0,
    pecas_defeituosas   INT NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_indicador_oee_maquina FOREIGN KEY (maquina_id) REFERENCES maquina(id) ON DELETE CASCADE,
    CONSTRAINT fk_indicador_oee_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
    CONSTRAINT chk_indicador_periodo CHECK (periodo_fim > periodo_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_indicador_oee_maquina_periodo
    ON indicador_oee (maquina_id, periodo_inicio DESC);

-- ---------------------------------------------------------------------
-- Tabela: leitura_planta
-- Cada mudança lida no /dadosPlanta, com o JSON bruto: todo OEE pode ser
-- rastreado até o dado que veio da bancada.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leitura_planta (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    maquina_id          BIGINT NOT NULL,
    lido_em             TIMESTAMP(3) NOT NULL,
    payload             JSON NOT NULL,

    CONSTRAINT fk_leitura_planta_maquina FOREIGN KEY (maquina_id) REFERENCES maquina(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_leitura_planta_maquina_lido_em
    ON leitura_planta (maquina_id, lido_em DESC);

-- ---------------------------------------------------------------------
-- Seed: a Bancada Smart 4.0 do laboratório, fixa no id = 1 (o frontend
-- e as requisições da API usam esse id).
-- tempo_padrao_pedido_s = 80 s: cronometragem da equipe (20 s estoque +
-- 35 s manipulação + 25 s separação), a mesma do data/ciclo_padrao.json e
-- de TEMPO_PADRAO_*_S no backend.
-- ---------------------------------------------------------------------
INSERT INTO maquina (id, identificador_bancada, nome, status_operacional, tempo_padrao_pedido_s, pecas_por_pedido)
VALUES (1, 'BANCADA_SMART_01', 'Bancada Smart 4.0 - Lab SENAI', 'SEM_CONEXAO', 80.000, 3)
ON DUPLICATE KEY UPDATE
    identificador_bancada = VALUES(identificador_bancada),
    nome = VALUES(nome),
    status_operacional = VALUES(status_operacional),
    tempo_padrao_pedido_s = VALUES(tempo_padrao_pedido_s),
    pecas_por_pedido = VALUES(pecas_por_pedido);

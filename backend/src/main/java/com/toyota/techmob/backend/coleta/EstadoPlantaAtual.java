package com.toyota.techmob.backend.coleta;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;

/**
 * Estado ao vivo da bancada, em memória. O ColetorPlanta escreve a cada
 * leitura e a API lê (GET /api/planta/{id}/status). AtomicReference: quem
 * lê sempre vê um estado inteiro, nunca metade de uma atualização.
 *
 * Sem coleta (desligada ou antes da primeira leitura), o estado é
 * SEM_CONEXAO.
 */
@Component
public class EstadoPlantaAtual {

    public record EstacaoAtual(Estacao estacao, String status, SituacaoEstacao situacao) {
    }

    /** Pedido aberto: pedidoId e numero vêm do banco, podem ser nulos se a gravação falhou. */
    public record PedidoAtual(Long pedidoId, Integer numero, Instant inicio, Estacao etapaAtual) {
    }

    /**
     * @param maquinaId       máquina coletada
     * @param conectado       a última leitura deu certo
     * @param fonte           origem da última leitura boa (null antes da primeira)
     * @param status          status da bancada
     * @param operacao        operacao_string da última leitura
     * @param estacoes        as 3 estações, na ordem da bancada
     * @param pedido          pedido aberto (null se a bancada está aguardando)
     * @param ultimaLeituraEm horário da última leitura boa
     */
    public record Estado(Long maquinaId, boolean conectado, OrigemPedido fonte, StatusMaquina status,
                         String operacao, List<EstacaoAtual> estacoes, PedidoAtual pedido,
                         Instant ultimaLeituraEm) {

        public static Estado semConexao(Long maquinaId) {
            List<EstacaoAtual> estacoes = Arrays.stream(Estacao.values())
                    .map(e -> new EstacaoAtual(e, null, SituacaoEstacao.SEM_CONEXAO))
                    .toList();
            return new Estado(maquinaId, false, null, StatusMaquina.SEM_CONEXAO, null, estacoes, null, null);
        }
    }

    private final AtomicReference<Estado> atual = new AtomicReference<>();

    public void atualizar(Estado estado) {
        atual.set(estado);
    }

    /** Estado da máquina; SEM_CONEXAO se ela não está sendo coletada. */
    public Estado da(Long maquinaId) {
        Estado estado = atual.get();
        return estado != null && estado.maquinaId().equals(maquinaId) ? estado : Estado.semConexao(maquinaId);
    }
}

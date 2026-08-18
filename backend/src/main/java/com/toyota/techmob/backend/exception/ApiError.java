package com.toyota.techmob.backend.exception;

import java.time.OffsetDateTime;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho
) {
}
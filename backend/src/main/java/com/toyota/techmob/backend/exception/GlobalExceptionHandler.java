package com.toyota.techmob.backend.exception;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Todo erro da API sai no mesmo formato (ApiError).
 *
 * 404: máquina ou pedido inexistente, ou rota inexistente.
 * 400: id ou parâmetro em formato inválido (ex.: /api/dashboard/abc, origem=XYZ).
 * 500: erro inesperado. O detalhe vai só para o log; a resposta leva uma
 * mensagem fixa, para não expor nada interno.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({MaquinaNotFoundException.class, PedidoNotFoundException.class})
    public ResponseEntity<ApiError> handleNaoEncontrado(RuntimeException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleRotaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso não encontrado", "Rota inexistente", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleParametroInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Requisição inválida", mensagemParametroInvalido(ex), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerico(Exception ex, HttpServletRequest request) {
        // Os outros erros do Spring MVC já trazem o status certo
        // (ex.: 405 para um POST numa rota que só aceita GET).
        if (ex instanceof ErrorResponse resposta && resposta.getStatusCode().is4xxClientError()) {
            return responder(resposta.getStatusCode(), "Requisição inválida",
                    "Requisição não aceita por esta rota", request);
        }
        logger.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Erro inesperado no servidor", request);
    }

    private static ResponseEntity<ApiError> responder(
            HttpStatusCode status, String erro, String mensagem, HttpServletRequest request) {
        ApiError corpo = new ApiError(OffsetDateTime.now(), status.value(), erro, mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }

    /** Diz qual parâmetro veio errado e, quando dá, o formato esperado. */
    private static String mensagemParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Valor inválido para '" + ex.getName() + "'";
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            String aceitos = Arrays.stream(tipo.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return mensagem + ". Valores aceitos: " + aceitos;
        }
        if (tipo == Instant.class) {
            return mensagem + ". Use data e hora ISO-8601, ex.: 2026-11-17T03:00:00Z";
        }
        return mensagem;
    }
}

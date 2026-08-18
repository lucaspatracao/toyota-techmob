package com.toyota.techmob.backend.exception;

public class MaquinaNotFoundException extends RuntimeException {

    public MaquinaNotFoundException(Long maquinaId) {
        super("Máquina não encontrada com id: " + maquinaId);
    }
}
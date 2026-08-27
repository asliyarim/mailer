package com.aksa.mailer.common.domain;

/**
 * Is kurali ihlali (orn. gecersiz content semasi). GlobalExceptionHandler
 * bunu 400'e cevirir. Framework bagimliligi YOK - domain katmaninda kalir.
 */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}

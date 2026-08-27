package com.aksa.mailer.common.domain;

/** Istenen kayit yok. GlobalExceptionHandler bunu 404'e cevirir. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

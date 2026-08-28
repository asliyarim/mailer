package com.aksa.mailer.common.web;

import com.aksa.mailer.common.domain.DomainValidationException;
import com.aksa.mailer.common.domain.NotFoundException;
import com.aksa.mailer.common.domain.VersionConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller'lar try/catch yazmasin diye tek yerde hata cevrimi.
 * Yeni bir domain istisnasi eklenirse karsiligi BURAYA yazilir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(NotFoundException e, HttpServletRequest request) {
        return yanit(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiErrorResponse> validation(DomainValidationException e, HttpServletRequest request) {
        return yanit(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /**
     * Takim yetkisi yok. CsrfCookieFilter'in 403'unden farkli: orada istek
     * bicimsel olarak reddediliyor, burada kullanicinin o veriye hakki yok.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> erisimYok(AccessDeniedException e, HttpServletRequest request) {
        return yanit(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<ApiErrorResponse> conflict(VersionConflictException e, HttpServletRequest request) {
        return yanit(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> beanValidation(MethodArgumentNotValidException e,
                                                           HttpServletRequest request) {
        String mesaj = e.getBindingResult().getFieldErrors().stream()
                .map(hata -> hata.getField() + ": " + hata.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Geçersiz istek.");
        return yanit(HttpStatus.BAD_REQUEST, mesaj, request);
    }

    /**
     * Bozuk/okunamayan istek govdesi. Burada yakalanmasa Spring'in kendi
     * govdesiz 400'u donerdi; o da /error'a forward edilip istemciye yanlis
     * kodla ulasma riskini tasir (bkz. SecurityConfig, ERROR dispatch notu).
     * Buradan donen yanit govdeli oldugu icin forward hic olmaz.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> okunamayanGovde(HttpMessageNotReadableException e,
                                                            HttpServletRequest request) {
        return yanit(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı: " + kokNeden(e), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> eksikParametre(MissingServletRequestParameterException e,
                                                           HttpServletRequest request) {
        return yanit(HttpStatus.BAD_REQUEST, "Zorunlu parametre eksik: " + e.getParameterName(), request);
    }

    /** Jackson'in ic ice sarilmis mesajlari uzun; en alttaki gercek nedeni al. */
    private String kokNeden(Throwable e) {
        Throwable neden = e;
        while (neden.getCause() != null) {
            neden = neden.getCause();
        }
        return neden.getMessage();
    }

    private ResponseEntity<ApiErrorResponse> yanit(HttpStatus status, String mesaj, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), status.getReasonPhrase(), mesaj, request.getRequestURI()));
    }
}

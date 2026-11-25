package com.quickbank.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = RestController.class)
public class ProblemDetailAdvice {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailAdvice.class);

    @ExceptionHandler(QuickBankException.class)
    public ResponseEntity<ProblemDetail> quickBank(QuickBankException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status()).body(Problems.of(ex, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> invalid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request is not valid.");
        return ResponseEntity.badRequest().body(Problems.validation(detail, request.getRequestURI()));
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> missing(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(Problems.validation(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled API error on {}", request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "QuickBank could not complete that request.");
        problem.setTitle("Unexpected error");
        problem.setType(java.net.URI.create("https://quickbank.dev/problems/unexpected"));
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return ResponseEntity.internalServerError().body(problem);
    }
}

package com.microfinancehub.notification_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Validation DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(
            MethodArgumentNotValidException ex) {

        Map<String, String> erreurs = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            erreurs.put(field, error.getDefaultMessage());
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("statut", 400);
        body.put("erreurs", erreurs);

        return ResponseEntity.badRequest().body(body);
    }

    // URL inexistante
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(
            NoResourceFoundException ex) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("statut", 404);
        body.put("message", "Endpoint introuvable");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Runtime métier
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> runtime(
            RuntimeException ex) {

        log.error("Erreur métier : {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("statut", 400);
        body.put("message", ex.getMessage());

        return ResponseEntity.badRequest().body(body);
    }

    // Erreur serveur réelle
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(
            Exception ex) {

        log.error("Erreur serveur", ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("statut", 500);
        body.put("message", "Erreur interne du serveur");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}

package tn.tuniheritage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> gererErreur(
            ResponseStatusException exception) {

        Map<String, Object> erreur = new LinkedHashMap<>();

        erreur.put("status", exception.getStatusCode().value());
        erreur.put("error", "Bad Request");
        erreur.put("message", exception.getReason());

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(erreur);
    }
}

package co.edu.unicauca.saberpro.question.web;

import co.edu.unicauca.saberpro.question.application.exception.InvalidQuestionStateException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionValidationException;
import co.edu.unicauca.saberpro.question.application.exception.ValidationServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QuestionValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(QuestionValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage(), "errors", e.getErrors()));
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(QuestionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage(), "errors", List.of()));
    }

    @ExceptionHandler(InvalidQuestionStateException.class)
    public ResponseEntity<Map<String, Object>> handleState(InvalidQuestionStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", e.getMessage(), "errors", List.of()));
    }

    @ExceptionHandler(ValidationServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(ValidationServiceUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", e.getMessage(), "errors", List.of()));
    }
}

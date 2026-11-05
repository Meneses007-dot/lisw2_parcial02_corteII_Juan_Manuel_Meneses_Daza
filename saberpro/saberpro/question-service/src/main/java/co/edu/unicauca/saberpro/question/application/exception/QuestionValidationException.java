package co.edu.unicauca.saberpro.question.application.exception;

import java.util.List;

public class QuestionValidationException extends RuntimeException {
    private final List<String> errors;

    public QuestionValidationException(List<String> errors) {
        super("La pregunta no superó la validación estructural.");
        this.errors = errors;
    }

    public List<String> getErrors() { return errors; }
}

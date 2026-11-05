package co.edu.unicauca.saberpro.question.application.exception;

public class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(Long id) {
        super("No existe la pregunta con id " + id);
    }
}

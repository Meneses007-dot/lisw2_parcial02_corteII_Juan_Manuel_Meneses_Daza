package co.edu.unicauca.saberpro.question.application.port;

import co.edu.unicauca.saberpro.question.domain.model.Question;

import java.util.List;

public interface QuestionValidationPort {

    record ValidationResult(boolean valid, List<String> errors) {}

    ValidationResult validate(Question question);
}

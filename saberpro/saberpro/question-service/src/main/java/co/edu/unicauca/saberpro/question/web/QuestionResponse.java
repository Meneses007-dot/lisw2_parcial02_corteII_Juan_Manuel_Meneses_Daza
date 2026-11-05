package co.edu.unicauca.saberpro.question.web;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;

import java.util.List;

public record QuestionResponse(Long id, String statement, List<OptionDto> options,
                               String correctKey, String justification, QuestionStatus status) {

    public static QuestionResponse from(Question q) {
        return new QuestionResponse(q.getId(), q.getStatement(),
                q.getOptions().stream().map(o -> new OptionDto(o.key(), o.text())).toList(),
                q.getCorrectKey(), q.getJustification(), q.getStatus());
    }
}

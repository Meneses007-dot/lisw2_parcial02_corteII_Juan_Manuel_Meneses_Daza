package co.edu.unicauca.saberpro.question.infrastructure.mapper;

import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.infrastructure.jpa.record.AnswerOptionRecord;
import co.edu.unicauca.saberpro.question.infrastructure.jpa.record.QuestionRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Mapeador bidireccional Question (dominio) <-> QuestionRecord (JPA). */
@Component
public class QuestionDomainMapper {

    public QuestionRecord toRecord(Question question) {
        QuestionRecord record = new QuestionRecord();
        record.setId(question.getId());
        record.setStatement(question.getStatement());
        record.setCorrectKey(question.getCorrectKey());
        record.setJustification(question.getJustification());
        record.setStatus(question.getStatus());
        List<AnswerOptionRecord> options = new ArrayList<>();
        for (AnswerOption o : question.getOptions()) {
            options.add(new AnswerOptionRecord(o.key(), o.text()));
        }
        record.setOptions(options);
        return record;
    }

    public Question toDomain(QuestionRecord record) {
        List<AnswerOption> options = new ArrayList<>();
        for (AnswerOptionRecord o : record.getOptions()) {
            options.add(new AnswerOption(o.getOptionKey(), o.getOptionText()));
        }
        return new Question(record.getId(), record.getStatement(), options,
                record.getCorrectKey(), record.getJustification(), record.getStatus());
    }
}

package co.edu.unicauca.saberpro.question.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de dominio PURO: sin @Entity, @Table ni @Id.
 * La persistencia se resuelve en infrastructure.jpa.record.QuestionRecord.
 */
public class Question {

    private Long id;
    private String statement;
    private List<AnswerOption> options;
    private String correctKey;
    private String justification;
    private QuestionStatus status;

    public Question(Long id, String statement, List<AnswerOption> options,
                    String correctKey, String justification, QuestionStatus status) {
        this.id = id;
        this.statement = statement;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
        this.correctKey = correctKey;
        this.justification = justification;
        this.status = status;
    }

    public void markAsDraft() {
        this.status = QuestionStatus.DRAFT;
    }

    public boolean isDraft() {
        return status == QuestionStatus.DRAFT;
    }

    public void changeStatus(QuestionStatus newStatus) {
        this.status = newStatus;
    }

    public Long getId() { return id; }
    public String getStatement() { return statement; }
    public List<AnswerOption> getOptions() { return List.copyOf(options); }
    public String getCorrectKey() { return correctKey; }
    public String getJustification() { return justification; }
    public QuestionStatus getStatus() { return status; }
}

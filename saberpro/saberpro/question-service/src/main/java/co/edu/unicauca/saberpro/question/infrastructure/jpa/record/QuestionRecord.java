package co.edu.unicauca.saberpro.question.infrastructure.jpa.record;

import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class QuestionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String statement;

    @Column(name = "correct_key", nullable = false)
    private String correctKey;

    @Column(nullable = false, length = 2000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id")
    private List<AnswerOptionRecord> options = new ArrayList<>();

    public QuestionRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public String getCorrectKey() { return correctKey; }
    public void setCorrectKey(String correctKey) { this.correctKey = correctKey; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public List<AnswerOptionRecord> getOptions() { return options; }
    public void setOptions(List<AnswerOptionRecord> options) { this.options = options; }
}

package co.edu.unicauca.saberpro.question.infrastructure.jpa.record;

import jakarta.persistence.*;

@Entity
@Table(name = "answer_options")
public class AnswerOptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "option_key", nullable = false)
    private String optionKey;

    @Column(name = "option_text", nullable = false, length = 1000)
    private String optionText;

    protected AnswerOptionRecord() {}

    public AnswerOptionRecord(String optionKey, String optionText) {
        this.optionKey = optionKey;
        this.optionText = optionText;
    }

    public Long getId() { return id; }
    public String getOptionKey() { return optionKey; }
    public String getOptionText() { return optionText; }
}

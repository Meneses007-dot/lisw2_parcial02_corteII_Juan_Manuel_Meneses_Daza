package co.edu.unicauca.saberpro.question.infrastructure.mapper;

import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import co.edu.unicauca.saberpro.question.infrastructure.jpa.record.QuestionRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionDomainMapperTest {

    private final QuestionDomainMapper mapper = new QuestionDomainMapper();

    private static Question domain(Long id, QuestionStatus status) {
        return new Question(id, "Cual es la capital de Colombia?",
                List.of(new AnswerOption("A", "Bogota"),
                        new AnswerOption("B", "Medellin"),
                        new AnswerOption("C", "Cali"),
                        new AnswerOption("D", "Popayan")),
                "A", "Bogota es la capital de Colombia y sede de los poderes publicos.", status);
    }

    @Test
    @DisplayName("toRecord copia todos los campos y las opciones")
    void toRecordMapsAllFields() {
        Question source = domain(11L, QuestionStatus.DRAFT);

        QuestionRecord record = mapper.toRecord(source);

        assertThat(record.getId()).isEqualTo(11L);
        assertThat(record.getStatement()).isEqualTo(source.getStatement());
        assertThat(record.getCorrectKey()).isEqualTo("A");
        assertThat(record.getJustification()).isEqualTo(source.getJustification());
        assertThat(record.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(record.getOptions())
                .hasSize(4)
                .extracting("optionKey", "optionText")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("A", "Bogota"),
                        org.assertj.core.groups.Tuple.tuple("B", "Medellin"),
                        org.assertj.core.groups.Tuple.tuple("C", "Cali"),
                        org.assertj.core.groups.Tuple.tuple("D", "Popayan"));
    }

    @Test
    @DisplayName("toDomain copia todos los campos y las opciones")
    void toDomainMapsAllFields() {
        QuestionRecord record = mapper.toRecord(domain(12L, QuestionStatus.PENDING_REVIEW));

        Question question = mapper.toDomain(record);

        assertThat(question.getId()).isEqualTo(12L);
        assertThat(question.getStatement()).isEqualTo(record.getStatement());
        assertThat(question.getCorrectKey()).isEqualTo("A");
        assertThat(question.getJustification()).isEqualTo(record.getJustification());
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING_REVIEW);
        assertThat(question.getOptions()).containsExactly(
                new AnswerOption("A", "Bogota"),
                new AnswerOption("B", "Medellin"),
                new AnswerOption("C", "Cali"),
                new AnswerOption("D", "Popayan"));
    }

    @Test
    @DisplayName("El mapeo es reversible (dominio -> record -> dominio)")
    void mappingIsReversible() {
        Question original = domain(13L, QuestionStatus.DRAFT);

        Question roundTrip = mapper.toDomain(mapper.toRecord(original));

        assertThat(roundTrip.getId()).isEqualTo(original.getId());
        assertThat(roundTrip.getStatement()).isEqualTo(original.getStatement());
        assertThat(roundTrip.getCorrectKey()).isEqualTo(original.getCorrectKey());
        assertThat(roundTrip.getJustification()).isEqualTo(original.getJustification());
        assertThat(roundTrip.getStatus()).isEqualTo(original.getStatus());
        assertThat(roundTrip.getOptions()).isEqualTo(original.getOptions());
    }

    @Test
    void newQuestionWithoutIdKeepsNullIdInRecord() {
        QuestionRecord record = mapper.toRecord(domain(null, QuestionStatus.DRAFT));

        assertThat(record.getId()).isNull();
    }

    @Test
    void nullStatusIsMappedAsNull() {
        QuestionRecord record = mapper.toRecord(domain(null, null));

        assertThat(record.getStatus()).isNull();
    }
}

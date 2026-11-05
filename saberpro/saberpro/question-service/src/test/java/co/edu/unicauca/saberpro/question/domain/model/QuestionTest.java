package co.edu.unicauca.saberpro.question.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTest {

    private static List<AnswerOption> options() {
        return List.of(
                new AnswerOption("A", "Bogota"),
                new AnswerOption("B", "Medellin"),
                new AnswerOption("C", "Cali"),
                new AnswerOption("D", "Popayan"));
    }

    @Test
    @DisplayName("markAsDraft deja la pregunta en estado DRAFT")
    void markAsDraftSetsDraftStatus() {
        Question question = new Question(null, "Enunciado", options(), "A", "Justificacion valida", null);

        question.markAsDraft();

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(question.isDraft()).isTrue();
    }

    @ParameterizedTest(name = "estado {0} no se considera DRAFT")
    @EnumSource(value = QuestionStatus.class, names = "PENDING_REVIEW")
    void statusOtherThanDraftIsNotDraft(QuestionStatus status) {
        Question question = new Question(1L, "Enunciado", options(), "A", "Justificacion valida", status);

        assertThat(question.isDraft()).isFalse();
    }

    @Test
    @DisplayName("changeStatus actualiza el estado y isDraft lo refleja")
    void changeStatusUpdatesState() {
        Question question = new Question(1L, "Enunciado", options(), "A", "Justificacion valida",
                QuestionStatus.DRAFT);

        question.changeStatus(QuestionStatus.PENDING_REVIEW);

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PENDING_REVIEW);
        assertThat(question.isDraft()).isFalse();
    }

    @Test
    void nullOptionsAreReplacedByEmptyList() {
        Question question = new Question(null, "Enunciado", null, "A", "Justificacion valida", null);

        assertThat(question.getOptions()).isEmpty();
    }

    @Test
    void constructorCopiesOptionsAndGetOptionsIsImmutable() {
        List<AnswerOption> mutable = new ArrayList<>(options());
        Question question = new Question(null, "Enunciado", mutable, "A", "Justificacion valida", null);

        mutable.clear();

        assertThat(question.getOptions()).hasSize(4);
        assertThatThrownBy(() -> question.getOptions().add(new AnswerOption("E", "Extra")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void gettersReturnConstructorValues() {
        Question question = new Question(9L, "Enunciado", options(), "B", "Una justificacion suficientemente larga",
                QuestionStatus.DRAFT);

        assertThat(question.getId()).isEqualTo(9L);
        assertThat(question.getStatement()).isEqualTo("Enunciado");
        assertThat(question.getCorrectKey()).isEqualTo("B");
        assertThat(question.getJustification()).isEqualTo("Una justificacion suficientemente larga");
        assertThat(question.getOptions()).containsExactlyElementsOf(options());
    }
}

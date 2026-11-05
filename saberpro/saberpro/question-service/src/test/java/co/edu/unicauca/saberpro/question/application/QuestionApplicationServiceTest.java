package co.edu.unicauca.saberpro.question.application;

import co.edu.unicauca.saberpro.question.application.command.SubmitQuestionForReview;
import co.edu.unicauca.saberpro.question.application.exception.InvalidQuestionStateException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionValidationException;
import co.edu.unicauca.saberpro.question.application.port.QuestionCommandPublisher;
import co.edu.unicauca.saberpro.question.application.port.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import co.edu.unicauca.saberpro.question.domain.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Pruebas del caso de uso con dobles de prueba escritos a mano
 * (sin Mockito, para no depender de instrumentacion bytecode).
 */
class QuestionApplicationServiceTest {

    private InMemoryQuestionRepository repository;
    private StubValidationPort validationPort;
    private RecordingCommandPublisher publisher;
    private QuestionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryQuestionRepository();
        validationPort = new StubValidationPort();
        publisher = new RecordingCommandPublisher();
        service = new QuestionApplicationService(repository, validationPort, publisher);
    }

    private static Question question(QuestionStatus status) {
        return new Question(null, "Cual es la capital de Colombia?",
                List.of(new AnswerOption("A", "Bogota"),
                        new AnswerOption("B", "Medellin"),
                        new AnswerOption("C", "Cali"),
                        new AnswerOption("D", "Popayan")),
                "A", "Bogota es la capital de Colombia y sede de los poderes publicos.", status);
    }

    private static Question storedQuestion(Long id, QuestionStatus status) {
        return new Question(id, "Cual es la capital de Colombia?",
                List.of(new AnswerOption("A", "Bogota"),
                        new AnswerOption("B", "Medellin"),
                        new AnswerOption("C", "Cali"),
                        new AnswerOption("D", "Popayan")),
                "A", "Bogota es la capital de Colombia y sede de los poderes publicos.", status);
    }

    @Test
    @DisplayName("create: si la validacion es exitosa marca DRAFT y persiste")
    void createValidQuestionPersistsAsDraft() {
        validationPort.result = new QuestionValidationPort.ValidationResult(true, List.of());

        Question saved = service.create(question(null));

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(repository.saveCount).isEqualTo(1);
        assertThat(repository.stored).containsValue(saved);
    }

    @Test
    @DisplayName("create: si la validacion falla lanza 400 y NO persiste")
    void createInvalidQuestionDoesNotPersist() {
        List<String> errors = List.of("error 1", "error 2");
        validationPort.result = new QuestionValidationPort.ValidationResult(false, errors);

        QuestionValidationException exception = catchThrowableOfType(
                () -> service.create(question(null)), QuestionValidationException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrors()).isEqualTo(errors);
        assertThat(repository.saveCount).isZero();
        assertThat(repository.stored).isEmpty();
    }

    @Test
    void getByIdReturnsQuestionWhenExists() {
        Question stored = storedQuestion(5L, QuestionStatus.DRAFT);
        repository.stored.put(5L, stored);

        assertThat(service.getById(5L)).isSameAs(stored);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("submitForReview: publica el comando con UUID y questionId correctos")
    void submitDraftPublishesCommand() {
        repository.stored.put(7L, storedQuestion(7L, QuestionStatus.DRAFT));

        String operationId = service.submitForReview(7L);

        assertThat(operationId).isNotBlank();
        assertThat(UUID.fromString(operationId)).isNotNull();
        assertThat(publisher.published).hasSize(1);
        SubmitQuestionForReview command = publisher.published.get(0);
        assertThat(command.operationId()).isEqualTo(operationId);
        assertThat(command.questionId()).isEqualTo(7L);
        assertThat(command.requestedAt()).isNotBlank();
        assertThatCode(() -> Instant.parse(command.requestedAt())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("submitForReview: una pregunta que no esta en DRAFT produce 409")
    void submitNonDraftIsRejected() {
        repository.stored.put(7L, storedQuestion(7L, QuestionStatus.PENDING_REVIEW));

        assertThatThrownBy(() -> service.submitForReview(7L))
                .isInstanceOf(InvalidQuestionStateException.class)
                .hasMessageContaining("DRAFT");

        assertThat(publisher.published).isEmpty();
    }

    @Test
    void submitMissingQuestionThrowsNotFound() {
        assertThatThrownBy(() -> service.submitForReview(123L))
                .isInstanceOf(QuestionNotFoundException.class);

        assertThat(publisher.published).isEmpty();
    }

    @Test
    void updateStatusChangesAndSaves() {
        Question stored = storedQuestion(3L, QuestionStatus.DRAFT);
        repository.stored.put(3L, stored);

        Question updated = service.updateStatus(3L, QuestionStatus.PENDING_REVIEW);

        assertThat(updated.getStatus()).isEqualTo(QuestionStatus.PENDING_REVIEW);
        assertThat(repository.saveCount).isEqualTo(1);
    }

    @Test
    void updateStatusOfMissingQuestionThrowsNotFound() {
        assertThatThrownBy(() -> service.updateStatus(3L, QuestionStatus.PENDING_REVIEW))
                .isInstanceOf(QuestionNotFoundException.class);

        assertThat(repository.saveCount).isZero();
    }

    // ---------------------------------------------------------------------
    // Dobles de prueba
    // ---------------------------------------------------------------------

    private static final class InMemoryQuestionRepository implements QuestionRepository {

        private final Map<Long, Question> stored = new HashMap<>();
        private int saveCount;
        private long sequence;

        @Override
        public Question save(Question question) {
            saveCount++;
            if (question.getId() == null) {
                Question withId = new Question(++sequence, question.getStatement(), question.getOptions(),
                        question.getCorrectKey(), question.getJustification(), question.getStatus());
                stored.put(withId.getId(), withId);
                return withId;
            }
            stored.put(question.getId(), question);
            return question;
        }

        @Override
        public Optional<Question> findById(Long id) {
            return Optional.ofNullable(stored.get(id));
        }
    }

    private static final class StubValidationPort implements QuestionValidationPort {

        private ValidationResult result = new ValidationResult(true, List.of());

        @Override
        public ValidationResult validate(Question question) {
            return result;
        }
    }

    private static final class RecordingCommandPublisher implements QuestionCommandPublisher {

        private final List<SubmitQuestionForReview> published = new ArrayList<>();

        @Override
        public void publish(SubmitQuestionForReview command) {
            published.add(command);
        }
    }
}

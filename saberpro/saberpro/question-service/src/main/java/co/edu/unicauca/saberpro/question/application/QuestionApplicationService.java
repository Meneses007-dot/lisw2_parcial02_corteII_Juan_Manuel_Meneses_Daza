package co.edu.unicauca.saberpro.question.application;

import co.edu.unicauca.saberpro.question.application.command.SubmitQuestionForReview;
import co.edu.unicauca.saberpro.question.application.exception.InvalidQuestionStateException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionValidationException;
import co.edu.unicauca.saberpro.question.application.port.QuestionCommandPublisher;
import co.edu.unicauca.saberpro.question.application.port.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import co.edu.unicauca.saberpro.question.domain.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class QuestionApplicationService {

    private final QuestionRepository repository;
    private final QuestionValidationPort validationPort;
    private final QuestionCommandPublisher publisher;

    public QuestionApplicationService(QuestionRepository repository,
                                      QuestionValidationPort validationPort,
                                      QuestionCommandPublisher publisher) {
        this.repository = repository;
        this.validationPort = validationPort;
        this.publisher = publisher;
    }

    /** Punto 1: valida de forma síncrona (REST) ANTES de persistir. */
    public Question create(Question question) {
        QuestionValidationPort.ValidationResult result = validationPort.validate(question);
        if (!result.valid()) {
            throw new QuestionValidationException(result.errors());
        }
        question.markAsDraft();
        return repository.save(question);
    }

    public Question getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new QuestionNotFoundException(id));
    }

    /** Punto 3: publica el comando y retorna el operationId sin esperar el resultado. */
    public String submitForReview(Long id) {
        Question question = getById(id);
        if (!question.isDraft()) {
            throw new InvalidQuestionStateException(
                    "Solo se pueden enviar a revisión preguntas en estado DRAFT (actual: " + question.getStatus() + ").");
        }
        String operationId = UUID.randomUUID().toString();
        publisher.publish(new SubmitQuestionForReview(operationId, id, Instant.now().toString()));
        return operationId;
    }

    /** Sincronización de estado invocada por Lifecycle Service (autoridad del estado). */
    public Question updateStatus(Long id, QuestionStatus status) {
        Question question = getById(id);
        question.changeStatus(status);
        return repository.save(question);
    }
}

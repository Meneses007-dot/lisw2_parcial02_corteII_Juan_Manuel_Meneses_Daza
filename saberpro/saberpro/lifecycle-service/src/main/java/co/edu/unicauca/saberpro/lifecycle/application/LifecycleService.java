package co.edu.unicauca.saberpro.lifecycle.application;

import co.edu.unicauca.saberpro.lifecycle.messaging.SubmitQuestionForReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Autoridad centralizada del estado de las preguntas. */
@Service
public class LifecycleService {

    private static final Logger log = LoggerFactory.getLogger(LifecycleService.class);
    public static final String PENDING_REVIEW = "PENDING_REVIEW";

    private final Map<Long, QuestionLifecycle> states = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private final String questionServiceUrl;

    public LifecycleService(@Value("${question.service.url}") String questionServiceUrl) {
        this.questionServiceUrl = questionServiceUrl;
    }

    public void transitionToPendingReview(SubmitQuestionForReview command) {
        QuestionLifecycle updated = new QuestionLifecycle(
                command.questionId(), PENDING_REVIEW, command.operationId(), Instant.now().toString());
        states.put(command.questionId(), updated);
        log.info("Pregunta {} -> {}", command.questionId(), PENDING_REVIEW);

        // Sincroniza el estado con Question Service; un fallo aquí no debe reencolar el mensaje.
        try {
            restTemplate.put(questionServiceUrl + "/questions/{id}/status",
                    Map.of("status", PENDING_REVIEW), command.questionId());
        } catch (Exception e) {
            log.warn("No se pudo sincronizar el estado con Question Service: {}", e.getMessage());
        }
    }

    public Optional<QuestionLifecycle> find(Long questionId) {
        return Optional.ofNullable(states.get(questionId));
    }

    public Collection<QuestionLifecycle> findAll() {
        return states.values();
    }
}

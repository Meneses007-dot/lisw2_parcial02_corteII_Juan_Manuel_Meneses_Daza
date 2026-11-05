package co.edu.unicauca.saberpro.lifecycle.messaging;

import co.edu.unicauca.saberpro.lifecycle.application.LifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionSubmitListenerTest {

    private RecordingLifecycleService lifecycleService;
    private QuestionSubmitListener listener;

    @BeforeEach
    void setUp() {
        lifecycleService = new RecordingLifecycleService();
        listener = new QuestionSubmitListener(lifecycleService);
    }

    @Test
    @DisplayName("El listener delega el comando recibido en LifecycleService")
    void delegatesCommandToLifecycleService() {
        SubmitQuestionForReview command =
                new SubmitQuestionForReview("op-1", 5L, "2026-11-05T10:00:00Z");

        listener.onSubmitQuestionForReview(command);

        assertThat(lifecycleService.received).isEqualTo(command);
    }

    /** Doble de prueba: registra el comando sin tocar RabbitMQ ni HTTP. */
    private static final class RecordingLifecycleService extends LifecycleService {

        private SubmitQuestionForReview received;

        private RecordingLifecycleService() {
            super("http://127.0.0.1:1");
        }

        @Override
        public void transitionToPendingReview(SubmitQuestionForReview command) {
            this.received = command;
        }
    }
}

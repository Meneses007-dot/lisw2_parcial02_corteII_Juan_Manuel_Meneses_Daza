package co.edu.unicauca.saberpro.lifecycle.application;

import co.edu.unicauca.saberpro.lifecycle.messaging.SubmitQuestionForReview;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Pruebas contra el codigo de produccion tal cual: LifecycleService crea su propio
 * RestTemplate, por lo que la sincronizacion HTTP se verifica con un servidor local
 * embebido del JDK (com.sun.net.httpserver) en un puerto efimero.
 */
class LifecycleServiceTest {

    private HttpServer server;
    private String baseUrl;

    private final AtomicReference<String> lastMethod = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/questions", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        lastMethod.set(exchange.getRequestMethod());
        lastPath.set(exchange.getRequestURI().getPath());
        lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private static SubmitQuestionForReview command(String operationId, Long questionId) {
        return new SubmitQuestionForReview(operationId, questionId, "2026-11-05T10:00:00Z");
    }

    @Test
    @DisplayName("transitionToPendingReview guarda el estado PENDING_REVIEW localmente")
    void transitionStoresPendingReviewState() {
        LifecycleService service = new LifecycleService(baseUrl);

        service.transitionToPendingReview(command("op-1", 7L));

        QuestionLifecycle state = service.find(7L).orElseThrow();
        assertThat(state.questionId()).isEqualTo(7L);
        assertThat(state.state()).isEqualTo("PENDING_REVIEW");
        assertThat(state.lastOperationId()).isEqualTo("op-1");
        assertThat(state.updatedAt()).isNotBlank();
    }

    @Test
    @DisplayName("transitionToPendingReview sincroniza el estado por PUT a /questions/{id}/status")
    void transitionSynchronizesWithQuestionService() {
        LifecycleService service = new LifecycleService(baseUrl);

        service.transitionToPendingReview(command("op-7", 7L));

        assertThat(requests).hasValue(1);
        assertThat(lastMethod).hasValue("PUT");
        assertThat(lastPath).hasValue("/questions/7/status");
        assertThat(lastBody.get()).contains("PENDING_REVIEW");
    }

    @Test
    @DisplayName("Si Question Service no responde, el estado local se guarda igual (sin excepcion)")
    void synchronizationFailureIsTolerated() {
        // Puerto 1: conexion rechazada de inmediato en Linux.
        LifecycleService service = new LifecycleService("http://127.0.0.1:1");

        assertThatCode(() -> service.transitionToPendingReview(command("op-9", 9L)))
                .doesNotThrowAnyException();

        assertThat(service.find(9L)).isPresent();
        assertThat(service.find(9L).orElseThrow().state()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void aNewCommandForTheSameQuestionOverwritesTheState() {
        LifecycleService service = new LifecycleService(baseUrl);

        service.transitionToPendingReview(command("op-a", 10L));
        service.transitionToPendingReview(command("op-b", 10L));

        assertThat(service.findAll()).hasSize(1);
        assertThat(service.find(10L).orElseThrow().lastOperationId()).isEqualTo("op-b");
        assertThat(requests).hasValue(2);
    }

    @Test
    void findReturnsEmptyForUnknownQuestion() {
        LifecycleService service = new LifecycleService(baseUrl);

        assertThat(service.find(123L)).isEmpty();
    }

    @Test
    void findAllReturnsEveryTrackedQuestion() {
        LifecycleService service = new LifecycleService(baseUrl);

        service.transitionToPendingReview(command("op-1", 1L));
        service.transitionToPendingReview(command("op-2", 2L));

        assertThat(service.findAll())
                .hasSize(2)
                .extracting(QuestionLifecycle::questionId)
                .containsExactlyInAnyOrder(1L, 2L);
    }
}

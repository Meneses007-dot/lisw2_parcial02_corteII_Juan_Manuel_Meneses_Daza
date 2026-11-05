package co.edu.unicauca.saberpro.question.web;

import co.edu.unicauca.saberpro.question.application.QuestionApplicationService;
import co.edu.unicauca.saberpro.question.application.exception.InvalidQuestionStateException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.application.exception.QuestionValidationException;
import co.edu.unicauca.saberpro.question.application.exception.ValidationServiceUnavailableException;
import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la capa web sin contexto de Spring ni Mockito:
 * controlador real + doble de QuestionApplicationService + MockMvc standalone.
 */
class QuestionControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeQuestionApplicationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new FakeQuestionApplicationService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static Question question(Long id, QuestionStatus status) {
        return new Question(id, "Cual es la capital de Colombia?",
                List.of(new AnswerOption("A", "Bogota"),
                        new AnswerOption("B", "Medellin"),
                        new AnswerOption("C", "Cali"),
                        new AnswerOption("D", "Popayan")),
                "A", "Bogota es la capital de Colombia y sede de los poderes publicos.", status);
    }

    private static Map<String, Object> validBody() {
        return Map.of(
                "statement", "Cual es la capital de Colombia?",
                "options", List.of(
                        Map.of("key", "A", "text", "Bogota"),
                        Map.of("key", "B", "text", "Medellin"),
                        Map.of("key", "C", "text", "Cali"),
                        Map.of("key", "D", "text", "Popayan")),
                "correctKey", "A",
                "justification", "Bogota es la capital de Colombia y sede de los poderes publicos.");
    }

    @Test
    @DisplayName("POST /questions valida -> 201 Created con estado DRAFT")
    void createReturns201() throws Exception {
        service.onCreate = q -> question(1L, QuestionStatus.DRAFT);

        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.options.length()").value(4));
    }

    @Test
    @DisplayName("POST /questions invalida -> 400 con message y errors[]")
    void createReturns400WhenValidationFails() throws Exception {
        service.onCreate = q -> {
            throw new QuestionValidationException(
                    List.of("La pregunta debe tener exactamente 4 opciones (recibidas: 3).",
                            "La justificacion es obligatoria."));
        };

        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0]").value("La pregunta debe tener exactamente 4 opciones (recibidas: 3)."));
    }

    @Test
    @DisplayName("POST /questions con Validation Service caido -> 503")
    void createReturns503WhenValidationServiceIsDown() throws Exception {
        service.onCreate = q -> {
            throw new ValidationServiceUnavailableException("Validation Service no disponible", null);
        };

        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Validation Service no disponible"));
    }

    @Test
    void getReturnsQuestion() throws Exception {
        service.onGetById = id -> question(id, QuestionStatus.PENDING_REVIEW);

        mockMvc.perform(get("/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        service.onGetById = id -> {
            throw new QuestionNotFoundException(id);
        };

        mockMvc.perform(get("/questions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /questions/{id}/submit -> 202 con operationId y PROCESSING")
    void submitReturns202() throws Exception {
        service.onSubmit = id -> "op-123";

        mockMvc.perform(post("/questions/1/submit"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.operationId").value("op-123"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("POST /questions/{id}/submit sobre una pregunta no DRAFT -> 409")
    void submitReturns409WhenInvalidState() throws Exception {
        service.onSubmit = id -> {
            throw new InvalidQuestionStateException("Solo se pueden enviar preguntas DRAFT");
        };

        mockMvc.perform(post("/questions/1/submit"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("PUT /questions/{id}/status sincroniza el estado (uso interno del Lifecycle)")
    void updateStatusReturnsUpdatedQuestion() throws Exception {
        service.onUpdateStatus = (id, newStatus) -> question(id, newStatus);

        mockMvc.perform(put("/questions/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    /** Doble de prueba: hereda del servicio real y sustituye solo los metodos usados por el controlador. */
    private static final class FakeQuestionApplicationService extends QuestionApplicationService {

        private Function<Question, Question> onCreate = q -> q;
        private Function<Long, Question> onGetById = id -> question(1L, QuestionStatus.DRAFT);
        private Function<Long, String> onSubmit = id -> "operation-id";
        private java.util.function.BiFunction<Long, QuestionStatus, Question> onUpdateStatus =
                (id, status) -> question(id, status);

        private FakeQuestionApplicationService() {
            super(null, null, null);
        }

        @Override
        public Question create(Question question) {
            return onCreate.apply(question);
        }

        @Override
        public Question getById(Long id) {
            return onGetById.apply(id);
        }

        @Override
        public String submitForReview(Long id) {
            return onSubmit.apply(id);
        }

        @Override
        public Question updateStatus(Long id, QuestionStatus status) {
            return onUpdateStatus.apply(id, status);
        }
    }
}

package co.edu.unicauca.saberpro.lifecycle.web;

import co.edu.unicauca.saberpro.lifecycle.application.LifecycleService;
import co.edu.unicauca.saberpro.lifecycle.application.QuestionLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la capa web sin contexto de Spring ni Mockito.
 */
class LifecycleControllerTest {

    private FakeLifecycleService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new FakeLifecycleService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LifecycleController(service))
                .build();
    }

    @Test
    void getByIdReturnsLifecycleState() throws Exception {
        service.byId = Optional.of(
                new QuestionLifecycle(7L, "PENDING_REVIEW", "op-1", "2026-11-05T10:00:00Z"));

        mockMvc.perform(get("/lifecycle/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(7))
                .andExpect(jsonPath("$.state").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.lastOperationId").value("op-1"));
    }

    @Test
    void getByIdReturns404WhenUnknown() throws Exception {
        service.byId = Optional.empty();

        mockMvc.perform(get("/lifecycle/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /lifecycle devuelve todos los estados registrados")
    void allReturnsTrackedStates() throws Exception {
        service.all = List.of(
                new QuestionLifecycle(1L, "PENDING_REVIEW", "op-1", "2026-11-05T10:00:00Z"),
                new QuestionLifecycle(2L, "PENDING_REVIEW", "op-2", "2026-11-05T10:01:00Z"));

        mockMvc.perform(get("/lifecycle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].questionId").value(1));
    }

    @Test
    void allReturnsEmptyArrayWhenNothingWasProcessed() throws Exception {
        service.all = List.of();

        mockMvc.perform(get("/lifecycle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /** Doble de prueba: hereda del servicio real y sustituye solo las consultas. */
    private static final class FakeLifecycleService extends LifecycleService {

        private Optional<QuestionLifecycle> byId = Optional.empty();
        private Collection<QuestionLifecycle> all = List.of();

        private FakeLifecycleService() {
            super("http://127.0.0.1:1");
        }

        @Override
        public Optional<QuestionLifecycle> find(Long questionId) {
            return byId;
        }

        @Override
        public Collection<QuestionLifecycle> findAll() {
            return all;
        }
    }
}

package co.edu.unicauca.saberpro.validation.web;

import co.edu.unicauca.saberpro.validation.domain.QuestionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la capa web sin levantar contexto de Spring ni usar Mockito:
 * se instancia el controlador real con el validador real y se usa MockMvc standalone.
 */
class ValidationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ValidationController(new QuestionValidator()))
                .build();
    }

    private static List<Map<String, Object>> options() {
        return List.of(
                Map.of("key", "A", "text", "Bogota"),
                Map.of("key", "B", "text", "Medellin"),
                Map.of("key", "C", "text", "Cali"),
                Map.of("key", "D", "text", "Popayan"));
    }

    @Test
    void validRequestReturns200() throws Exception {
        Map<String, Object> body = Map.of(
                "statement", "Cual es la capital de Colombia?",
                "options", options(),
                "correctKey", "A",
                "justification", "Bogota es la capital de Colombia y sede de los poderes publicos.");

        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void invalidRequestReturns400WithErrors() throws Exception {
        Map<String, Object> body = Map.of(
                "statement", "",
                "options", List.of(Map.of("key", "A", "text", "Uno")),
                "correctKey", "Z",
                "justification", "corta");

        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    @DisplayName("Un cuerpo JSON nulo es rechazado con 400 antes de llegar al validador")
    void nullBodyReturns400() throws Exception {
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());
    }
}

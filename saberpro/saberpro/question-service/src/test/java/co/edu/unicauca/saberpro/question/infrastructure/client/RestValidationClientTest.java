package co.edu.unicauca.saberpro.question.infrastructure.client;

import co.edu.unicauca.saberpro.question.application.exception.ValidationServiceUnavailableException;
import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestValidationClientTest {

    private static final String BASE_URL = "http://localhost:8082";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RestValidationClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RestValidationClient(restTemplate, BASE_URL);
    }

    private static Question question() {
        return new Question(1L, "Cual es la capital de Colombia?",
                List.of(new AnswerOption("A", "Bogota"),
                        new AnswerOption("B", "Medellin"),
                        new AnswerOption("C", "Cali"),
                        new AnswerOption("D", "Popayan")),
                "A", "Bogota es la capital de Colombia y sede de los poderes publicos.",
                QuestionStatus.DRAFT);
    }

    @Test
    @DisplayName("Envia el payload correcto al endpoint /validate")
    void sendsExpectedPayload() {
        server.expect(requestTo(BASE_URL + "/validate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.statement").value("Cual es la capital de Colombia?"))
                .andExpect(jsonPath("$.correctKey").value("A"))
                .andExpect(jsonPath("$.options.length()").value(4))
                .andExpect(jsonPath("$.options[0].key").value("A"))
                .andRespond(withSuccess("{\"valid\":true,\"errors\":[]}", MediaType.APPLICATION_JSON));

        var result = client.validate(question());

        server.verify();
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("200 con valid=false devuelve los errores del validador")
    void validFalseResponseReturnsErrors() {
        server.expect(requestTo(BASE_URL + "/validate"))
                .andRespond(withSuccess(
                        "{\"valid\":false,\"errors\":[\"error A\",\"error B\"]}",
                        MediaType.APPLICATION_JSON));

        var result = client.validate(question());

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).containsExactly("error A", "error B");
    }

    @Test
    @DisplayName("400 del Validation Service se traduce a resultado invalido con errores")
    void badRequestReturnsErrors() {
        server.expect(requestTo(BASE_URL + "/validate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"valid\":false,\"errors\":[\"La justificacion es obligatoria.\"]}"));

        var result = client.validate(question());

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).containsExactly("La justificacion es obligatoria.");
    }

    @Test
    @DisplayName("400 sin errores devuelve un error generico")
    void badRequestWithoutErrorsReturnsGenericError() {
        server.expect(requestTo(BASE_URL + "/validate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"valid\":false,\"errors\":[]}"));

        var result = client.validate(question());

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).contains("estructural");
    }

    @Test
    @DisplayName("Un fallo de red se traduce a ValidationServiceUnavailableException (503)")
    void connectionFailureThrowsUnavailable() {
        // RestTemplate real cuyo request factory simula una conexion rechazada.
        ClientHttpRequestFactory failingFactory = new ClientHttpRequestFactory() {
            @Override
            public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
                throw new ResourceAccessException("Connection refused");
            }
        };
        RestValidationClient failingClient = new RestValidationClient(new RestTemplate(failingFactory), BASE_URL);

        assertThatThrownBy(() -> failingClient.validate(question()))
                .isInstanceOf(ValidationServiceUnavailableException.class)
                .hasMessageContaining("no disponible");
    }
}

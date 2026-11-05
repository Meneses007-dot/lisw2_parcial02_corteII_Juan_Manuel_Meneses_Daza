package co.edu.unicauca.saberpro.question.infrastructure.client;

import co.edu.unicauca.saberpro.question.application.exception.ValidationServiceUnavailableException;
import co.edu.unicauca.saberpro.question.application.port.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/** Invocación síncrona HTTP REST: POST {validation.service.url}/validate */
@Component
public class RestValidationClient implements QuestionValidationPort {

    record OptionPayload(String key, String text) {}
    record ValidatePayload(String statement, List<OptionPayload> options, String correctKey, String justification) {}
    record ValidateResponse(boolean valid, List<String> errors) {}

    private final RestTemplate restTemplate;
    private final String validationUrl;

    public RestValidationClient(RestTemplate restTemplate,
                                @Value("${validation.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.validationUrl = baseUrl + "/validate";
    }

    @Override
    public ValidationResult validate(Question question) {
        List<OptionPayload> options = question.getOptions().stream()
                .map((AnswerOption o) -> new OptionPayload(o.key(), o.text()))
                .toList();
        ValidatePayload payload = new ValidatePayload(
                question.getStatement(), options, question.getCorrectKey(), question.getJustification());
        try {
            ResponseEntity<ValidateResponse> response =
                    restTemplate.postForEntity(validationUrl, payload, ValidateResponse.class);
            ValidateResponse body = response.getBody();
            if (body == null) {
                return new ValidationResult(true, List.of());
            }
            return new ValidationResult(body.valid(), body.errors() == null ? List.of() : body.errors());
        } catch (HttpClientErrorException.BadRequest e) {
            // 400 del Validation Service: se propaga la lista de errores
            ValidateResponse body = e.getResponseBodyAs(ValidateResponse.class);
            List<String> errors = (body == null || body.errors() == null || body.errors().isEmpty())
                    ? List.of("La pregunta no superó la validación estructural.")
                    : body.errors();
            return new ValidationResult(false, errors);
        } catch (ResourceAccessException e) {
            throw new ValidationServiceUnavailableException(
                    "Validation Service no disponible en " + validationUrl, e);
        }
    }
}

package co.edu.unicauca.saberpro.validation.web;

import co.edu.unicauca.saberpro.validation.domain.QuestionValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ValidationController {

    private final QuestionValidator validator;

    public ValidationController(QuestionValidator validator) {
        this.validator = validator;
    }

    /** 200 OK si es válida; 400 Bad Request con la lista de errores si no. */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(@RequestBody ValidationRequest request) {
        List<String> errors = validator.validate(request);
        if (errors.isEmpty()) {
            return ResponseEntity.ok(new ValidationResponse(true, errors));
        }
        return ResponseEntity.badRequest().body(new ValidationResponse(false, errors));
    }
}

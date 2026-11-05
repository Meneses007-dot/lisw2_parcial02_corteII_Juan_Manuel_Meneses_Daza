package co.edu.unicauca.saberpro.question.web;

import co.edu.unicauca.saberpro.question.application.QuestionApplicationService;
import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionApplicationService service;

    public QuestionController(QuestionApplicationService service) {
        this.service = service;
    }

    /** 201 Created si valida; 400 Bad Request (con errores) si no. */
    @PostMapping
    public ResponseEntity<QuestionResponse> create(@RequestBody CreateQuestionRequest request) {
        List<AnswerOption> options = new ArrayList<>();
        if (request.options() != null) {
            request.options().forEach(o -> options.add(new AnswerOption(o.key(), o.text())));
        }
        Question toCreate = new Question(null, request.statement(), options,
                request.correctKey(), request.justification(), null);
        Question saved = service.create(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestionResponse.from(saved));
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable Long id) {
        return QuestionResponse.from(service.getById(id));
    }

    /** 202 Accepted inmediato: el comando se procesa en segundo plano. */
    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmitResponse> submit(@PathVariable Long id) {
        String operationId = service.submitForReview(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new SubmitResponse(operationId, "PROCESSING"));
    }

    /** Endpoint interno: Lifecycle Service sincroniza el estado final. */
    @PutMapping("/{id}/status")
    public QuestionResponse updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        return QuestionResponse.from(service.updateStatus(id, request.status()));
    }
}

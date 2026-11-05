package co.edu.unicauca.saberpro.lifecycle.web;

import co.edu.unicauca.saberpro.lifecycle.application.LifecycleService;
import co.edu.unicauca.saberpro.lifecycle.application.QuestionLifecycle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/lifecycle")
public class LifecycleController {

    private final LifecycleService service;

    public LifecycleController(LifecycleService service) {
        this.service = service;
    }

    @GetMapping
    public Collection<QuestionLifecycle> all() {
        return service.findAll();
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionLifecycle> byId(@PathVariable Long questionId) {
        return service.find(questionId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}

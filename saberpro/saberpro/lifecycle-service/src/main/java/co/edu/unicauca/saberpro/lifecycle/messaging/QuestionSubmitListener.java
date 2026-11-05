package co.edu.unicauca.saberpro.lifecycle.messaging;

import co.edu.unicauca.saberpro.lifecycle.application.LifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class QuestionSubmitListener {

    private static final Logger log = LoggerFactory.getLogger(QuestionSubmitListener.class);
    private final LifecycleService lifecycleService;

    public QuestionSubmitListener(LifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onSubmitQuestionForReview(SubmitQuestionForReview command) {
        log.info("Comando recibido: operationId={}, questionId={}", command.operationId(), command.questionId());
        lifecycleService.transitionToPendingReview(command);
    }
}

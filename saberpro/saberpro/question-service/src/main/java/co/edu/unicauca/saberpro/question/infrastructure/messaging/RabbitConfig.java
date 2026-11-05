package co.edu.unicauca.saberpro.question.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "saberpro.direct.exchange";
    public static final String QUEUE = "question.submit.review.queue";
    public static final String ROUTING_KEY = "question.submit";

    @Bean
    public DirectExchange saberproExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue submitReviewQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding submitReviewBinding(Queue submitReviewQueue, DirectExchange saberproExchange) {
        return BindingBuilder.bind(submitReviewQueue).to(saberproExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

package co.edu.unicauca.saberpro.question.application.command;

/** Comando asíncrono publicado a RabbitMQ. */
public record SubmitQuestionForReview(String operationId, Long questionId, String requestedAt) {}

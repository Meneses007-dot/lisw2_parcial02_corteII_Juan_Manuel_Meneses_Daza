package co.edu.unicauca.saberpro.lifecycle.messaging;

/** Mismo contrato que el comando publicado por Question Service. */
public record SubmitQuestionForReview(String operationId, Long questionId, String requestedAt) {}

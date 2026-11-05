package co.edu.unicauca.saberpro.lifecycle.application;

public record QuestionLifecycle(Long questionId, String state, String lastOperationId, String updatedAt) {}

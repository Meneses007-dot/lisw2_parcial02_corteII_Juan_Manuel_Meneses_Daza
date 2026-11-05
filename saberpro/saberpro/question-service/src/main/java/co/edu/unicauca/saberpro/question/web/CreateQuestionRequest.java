package co.edu.unicauca.saberpro.question.web;

import java.util.List;

public record CreateQuestionRequest(String statement, List<OptionDto> options, String correctKey, String justification) {}

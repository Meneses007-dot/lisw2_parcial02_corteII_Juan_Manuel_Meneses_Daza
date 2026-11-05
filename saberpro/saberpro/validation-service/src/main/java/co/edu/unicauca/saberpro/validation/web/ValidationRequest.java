package co.edu.unicauca.saberpro.validation.web;

import java.util.List;

public record ValidationRequest(String statement, List<OptionDto> options, String correctKey, String justification) {}

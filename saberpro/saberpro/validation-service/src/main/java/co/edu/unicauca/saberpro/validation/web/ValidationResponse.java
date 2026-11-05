package co.edu.unicauca.saberpro.validation.web;

import java.util.List;

public record ValidationResponse(boolean valid, List<String> errors) {}

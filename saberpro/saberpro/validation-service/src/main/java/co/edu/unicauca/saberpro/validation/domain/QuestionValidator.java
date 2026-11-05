package co.edu.unicauca.saberpro.validation.domain;

import co.edu.unicauca.saberpro.validation.web.OptionDto;
import co.edu.unicauca.saberpro.validation.web.ValidationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reglas de validación estructural de una pregunta (HU03). */
@Component
public class QuestionValidator {

    public static final int REQUIRED_OPTIONS = 4;
    public static final int MIN_JUSTIFICATION_LENGTH = 20;

    public List<String> validate(ValidationRequest req) {
        List<String> errors = new ArrayList<>();

        if (req == null) {
            errors.add("El cuerpo de la solicitud es obligatorio.");
            return errors;
        }
        if (isBlank(req.statement())) {
            errors.add("El enunciado de la pregunta es obligatorio.");
        }

        List<OptionDto> options = req.options();
        Set<String> keys = new HashSet<>();
        if (options == null || options.size() != REQUIRED_OPTIONS) {
            int size = options == null ? 0 : options.size();
            errors.add("La pregunta debe tener exactamente " + REQUIRED_OPTIONS
                    + " opciones (recibidas: " + size + ").");
        }
        if (options != null) {
            for (OptionDto o : options) {
                if (o == null || isBlank(o.key()) || isBlank(o.text())) {
                    errors.add("Cada opción debe tener 'key' y 'text' no vacíos.");
                    continue;
                }
                if (!keys.add(o.key().trim())) {
                    errors.add("La clave de opción '" + o.key() + "' está duplicada.");
                }
            }
        }

        if (isBlank(req.correctKey())) {
            errors.add("La clave correcta es obligatoria.");
        } else if (!keys.contains(req.correctKey().trim())) {
            errors.add("La clave correcta '" + req.correctKey() + "' no existe entre las opciones.");
        }

        if (isBlank(req.justification())) {
            errors.add("La justificación es obligatoria.");
        } else if (req.justification().trim().length() < MIN_JUSTIFICATION_LENGTH) {
            errors.add("La justificación debe tener al menos " + MIN_JUSTIFICATION_LENGTH + " caracteres.");
        }
        return errors;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

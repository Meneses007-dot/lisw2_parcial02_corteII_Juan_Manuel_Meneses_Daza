package co.edu.unicauca.saberpro.validation.domain;

import co.edu.unicauca.saberpro.validation.web.OptionDto;
import co.edu.unicauca.saberpro.validation.web.ValidationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionValidatorTest {

    private static final String JUSTIFICATION =
            "Bogota es la capital de Colombia y sede de los poderes publicos.";

    private QuestionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new QuestionValidator();
    }

    private static List<OptionDto> fourOptions() {
        return new ArrayList<>(List.of(
                new OptionDto("A", "Bogota"),
                new OptionDto("B", "Medellin"),
                new OptionDto("C", "Cali"),
                new OptionDto("D", "Popayan")));
    }

    private static ValidationRequest validRequest() {
        return new ValidationRequest(
                "Cual es la capital de Colombia?", fourOptions(), "A", JUSTIFICATION);
    }

    private static ValidationRequest requestWith(List<OptionDto> options, String correctKey, String justification) {
        return new ValidationRequest("Enunciado", options, correctKey, justification);
    }

    @Test
    @DisplayName("Una pregunta completa y correcta no genera errores")
    void validQuestionHasNoErrors() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    @DisplayName("Un cuerpo nulo devuelve un error y no lanza excepcion")
    void nullRequestReturnsError() {
        assertThat(validator.validate(null))
                .singleElement()
                .asString()
                .contains("cuerpo");
    }

    @Nested
    @DisplayName("Regla: enunciado obligatorio")
    class StatementRules {

        @ParameterizedTest(name = "enunciado=[{0}] es rechazado")
        @ValueSource(strings = {"", "   "})
        void blankStatementIsRejected(String statement) {
            ValidationRequest request = new ValidationRequest(statement, fourOptions(), "A", JUSTIFICATION);

            assertThat(validator.validate(request))
                    .anyMatch(error -> error.contains("enunciado"));
        }

        @Test
        void nullStatementIsRejected() {
            ValidationRequest request = new ValidationRequest(null, fourOptions(), "A", JUSTIFICATION);

            assertThat(validator.validate(request))
                    .anyMatch(error -> error.contains("enunciado"));
        }
    }

    @Nested
    @DisplayName("Regla: exactamente 4 opciones")
    class OptionsCountRules {

        @Test
        void nullOptionsAreReported() {
            assertThat(validator.validate(requestWith(null, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("exactamente 4") && error.contains("recibidas: 0"));
        }

        @Test
        void emptyOptionsAreReported() {
            assertThat(validator.validate(requestWith(new ArrayList<>(), "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("exactamente 4") && error.contains("recibidas: 0"));
        }

        @Test
        void threeOptionsAreRejected() {
            List<OptionDto> three = fourOptions().subList(0, 3);

            assertThat(validator.validate(requestWith(three, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("exactamente 4") && error.contains("recibidas: 3"));
        }

        @Test
        void fiveOptionsAreRejected() {
            List<OptionDto> five = new ArrayList<>(fourOptions());
            five.add(new OptionDto("E", "Cartagena"));

            assertThat(validator.validate(requestWith(five, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("exactamente 4") && error.contains("recibidas: 5"));
        }
    }

    @Nested
    @DisplayName("Regla: opciones con clave y texto no vacios")
    class OptionContentRules {

        @Test
        void optionWithNullKeyIsRejected() {
            List<OptionDto> options = fourOptions();
            options.set(0, new OptionDto(null, "Bogota"));

            assertThat(validator.validate(requestWith(options, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("'key' y 'text'"));
        }

        @Test
        void optionWithBlankTextIsRejected() {
            List<OptionDto> options = fourOptions();
            options.set(1, new OptionDto("B", "  "));

            assertThat(validator.validate(requestWith(options, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("'key' y 'text'"));
        }

        @Test
        void nullOptionElementIsRejected() {
            List<OptionDto> options = fourOptions();
            options.set(2, null);

            assertThat(validator.validate(requestWith(options, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("'key' y 'text'"));
        }
    }

    @Nested
    @DisplayName("Regla: claves de opcion unicas")
    class UniqueKeysRules {

        @Test
        void duplicatedKeyIsRejected() {
            List<OptionDto> options = Arrays.asList(
                    new OptionDto("A", "Bogota"),
                    new OptionDto("A", "Medellin"),
                    new OptionDto("C", "Cali"),
                    new OptionDto("D", "Popayan"));

            assertThat(validator.validate(requestWith(options, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("duplicada") && error.contains("'A'"));
        }

        @Test
        void duplicatedKeyIsDetectedAfterTrimming() {
            List<OptionDto> options = Arrays.asList(
                    new OptionDto("A", "Bogota"),
                    new OptionDto(" A ", "Medellin"),
                    new OptionDto("C", "Cali"),
                    new OptionDto("D", "Popayan"));

            assertThat(validator.validate(requestWith(options, "A", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("duplicada"));
        }
    }

    @Nested
    @DisplayName("Regla: correctKey existente")
    class CorrectKeyRules {

        @Test
        void nullCorrectKeyIsRejected() {
            assertThat(validator.validate(requestWith(fourOptions(), null, JUSTIFICATION)))
                    .anyMatch(error -> error.contains("clave correcta es obligatoria"));
        }

        @Test
        void unknownCorrectKeyIsRejected() {
            assertThat(validator.validate(requestWith(fourOptions(), "Z", JUSTIFICATION)))
                    .anyMatch(error -> error.contains("no existe entre las opciones"));
        }

        @Test
        void correctKeyIsAcceptedWithSurroundingSpaces() {
            assertThat(validator.validate(requestWith(fourOptions(), " A ", JUSTIFICATION))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Regla: justificacion de al menos 20 caracteres")
    class JustificationRules {

        @Test
        void nullJustificationIsRejected() {
            assertThat(validator.validate(requestWith(fourOptions(), "A", null)))
                    .anyMatch(error -> error.contains("justificación es obligatoria"));
        }

        @Test
        void blankJustificationIsRejected() {
            assertThat(validator.validate(requestWith(fourOptions(), "A", "    ")))
                    .anyMatch(error -> error.contains("justificación es obligatoria"));
        }

        @Test
        void shortJustificationIsRejected() {
            assertThat(validator.validate(requestWith(fourOptions(), "A", "Muy corta")))
                    .anyMatch(error -> error.contains("al menos 20 caracteres"));
        }

        @Test
        void justificationWithExactlyTwentyCharactersIsAccepted() {
            String twenty = "12345678901234567890";

            assertThat(validator.validate(requestWith(fourOptions(), "A", twenty))).isEmpty();
        }

        @Test
        void spacesAreNotCountedAsJustificationContent() {
            String shortButPadded = "   texto corto   ";

            assertThat(validator.validate(requestWith(fourOptions(), "A", shortButPadded)))
                    .anyMatch(error -> error.contains("al menos 20 caracteres"));
        }
    }

    @Test
    @DisplayName("Una pregunta invalida por varias razones acumula todos los errores")
    void accumulatesAllErrors() {
        ValidationRequest request = new ValidationRequest(
                "", fourOptions().subList(0, 2), "Z", "corta");

        assertThat(validator.validate(request))
                .hasSize(4)
                .anyMatch(error -> error.contains("enunciado"))
                .anyMatch(error -> error.contains("exactamente 4"))
                .anyMatch(error -> error.contains("no existe entre las opciones"))
                .anyMatch(error -> error.contains("al menos 20 caracteres"));
    }
}

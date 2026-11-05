# Banco de Preguntas Saber PRO — Parcial 2 (Corte II)
## Requisitos
Java 17+, Maven 3.9+, Docker (para RabbitMQ) y Postman.

## Ejecución (4 terminales)
```bash
docker compose up -d                      # RabbitMQ  → http://localhost:15672 (guest/guest)
cd validation-service && mvn spring-boot:run
cd question-service   && mvn spring-boot:run
cd lifecycle-service  && mvn spring-boot:run
```
Sin Docker: instale RabbitMQ localmente en el puerto 5672.

## Flujo
1. `POST :8083/questions` → Question Service llama por `RestTemplate` a `POST :8082/validate`.
   - válida → estado `DRAFT`, se guarda, **201 Created**
   - inválida → no se guarda, **400** `{message, errors[]}`
2. `POST :8083/questions/{id}/submit` → publica `SubmitQuestionForReview` en `saberpro.direct.exchange` (routing key `question.submit`) y responde **202** `{operationId, status:"PROCESSING"}`.
3. Lifecycle Service consume `question.submit.review.queue`, pasa la pregunta a `PENDING_REVIEW` (`GET :8084/lifecycle/{id}`) y sincroniza el estado con Question Service (`PUT /questions/{id}/status`).

## Reglas de validación (Validation Service)
Enunciado no vacío · exactamente 4 opciones con clave/texto no vacíos y claves únicas · `correctKey` existente entre las claves · justificación no vacía de al menos 20 caracteres.

## Arquitectura limpia en Question Service
```
domain.model            Question, AnswerOption, QuestionStatus   (Java puro, sin @Entity)
domain.repository       QuestionRepository (interfaz)
application             QuestionApplicationService, puertos, comando, excepciones
infrastructure.jpa.record       QuestionRecord (@Entity @Table("questions"), @OneToMany cascade ALL), AnswerOptionRecord
infrastructure.mapper           QuestionDomainMapper (dominio <-> record)
infrastructure.jpa.repository   QuestionRepositoryJpaImpl -> SpringQuestionDataRepository (H2)
infrastructure.client           RestValidationClient (RestTemplate)
infrastructure.messaging        RabbitConfig, RabbitCommandPublisher (RabbitTemplate)
web                     QuestionController, DTOs, GlobalExceptionHandler
```
Consola H2: http://localhost:8083/h2-console · JDBC `jdbc:h2:mem:questiondb` · usuario `sa`.

## Pruebas

### Unitarias (JUnit 5)
```bash
cd validation-service && mvn test   # validador + controller
cd question-service   && mvn test   # dominio, caso de uso, mapper, cliente REST y controller
cd lifecycle-service  && mvn test   # estado, listener y controller
```
### Postman
Importar `postman/SaberPRO_Parcial2.postman_collection.json` y ejecutar con el Collection Runner en orden:
1. Crear válida → 201 · 2. Crear inválida → 400 · 3. Submit → 202 con operationId · 4a/4b/4c verificación en RabbitMQ (API de gestión), Lifecycle y Question.

>-|-|0>


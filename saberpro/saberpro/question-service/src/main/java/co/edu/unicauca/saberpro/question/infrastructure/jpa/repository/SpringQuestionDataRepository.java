package co.edu.unicauca.saberpro.question.infrastructure.jpa.repository;

import co.edu.unicauca.saberpro.question.infrastructure.jpa.record.QuestionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringQuestionDataRepository extends JpaRepository<QuestionRecord, Long> {
}

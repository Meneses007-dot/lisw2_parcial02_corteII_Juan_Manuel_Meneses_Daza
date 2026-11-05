package co.edu.unicauca.saberpro.question.infrastructure.jpa.repository;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.repository.QuestionRepository;
import co.edu.unicauca.saberpro.question.infrastructure.mapper.QuestionDomainMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class QuestionRepositoryJpaImpl implements QuestionRepository {

    private final SpringQuestionDataRepository dataRepository;
    private final QuestionDomainMapper mapper;

    public QuestionRepositoryJpaImpl(SpringQuestionDataRepository dataRepository, QuestionDomainMapper mapper) {
        this.dataRepository = dataRepository;
        this.mapper = mapper;
    }

    @Override
    public Question save(Question question) {
        return mapper.toDomain(dataRepository.save(mapper.toRecord(question)));
    }

    @Override
    public Optional<Question> findById(Long id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }
}

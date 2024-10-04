package id.co.anudbservice.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class CRUDServiceImpl<T, ID> implements CRUDService<T, ID> {

    private final JpaRepository<T, ID> repository;
    private final JpaSpecificationExecutor<T> specificationExecutor;

    public interface CriteriaFunction<T> {
        Predicate load(
                Root<T> root,
                CriteriaQuery<?> query,
                CriteriaBuilder criteriaBuilder
        );
    }

    @Override
    public T save(T entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(ID id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<T> getEntityByCriteria(CriteriaFunction<T> criteriaFunction) {
        return specificationExecutor.findOne(criteriaFunction::load);
    }

    @Override
    public List<T> getEntitiesByCriteria(CriteriaFunction<T> criteriaFunction) {
        return specificationExecutor.findAll(criteriaFunction::load);
    }
}

package id.co.anudbservice.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CRUDService<T, ID> {

    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);

    Optional<T> getEntityByCriteria(CRUDServiceImpl.CriteriaFunction<T> criteriaFunction);

    List<T> getEntitiesByCriteria(CRUDServiceImpl.CriteriaFunction<T> criteriaFunction);
}

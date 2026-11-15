package com.agrodata.repository;

import com.agrodata.model.EmailProdutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmailProdutorRepository extends JpaRepository<EmailProdutor, Long> {
    List<EmailProdutor> findByIdProdutor(Long idProdutor);
}

package com.agrodata.repository;

import com.agrodata.model.TelefoneProdutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TelefoneProdutorRepository extends JpaRepository<TelefoneProdutor, Long> {
    List<TelefoneProdutor> findByIdProdutor(Long idProdutor);
}

package com.agrodata.repository;

import com.agrodata.model.ComandoIrrigacao;
import com.agrodata.model.StatusComando;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ComandoIrrigacaoRepository extends JpaRepository<ComandoIrrigacao, Long> {
    List<ComandoIrrigacao> findByIdTalhaoAndStatusIn(Long idTalhao, Collection<StatusComando> status);
    List<ComandoIrrigacao> findByStatusInOrderByCriadoEmAsc(Collection<StatusComando> status);
}

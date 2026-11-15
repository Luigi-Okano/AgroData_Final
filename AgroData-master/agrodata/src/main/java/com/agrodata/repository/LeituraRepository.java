package com.agrodata.repository;

import com.agrodata.model.LeituraSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeituraRepository extends JpaRepository<LeituraSensor, Long> {
    List<LeituraSensor> findTop10ByIdSensorOrderByDataHoraDesc(Long idSensor);
    List<LeituraSensor> findByIdSensorOrderByDataHoraDesc(Long idSensor);
    Optional<LeituraSensor> findFirstByIdSensorOrderByDataHoraDesc(Long idSensor);
    long countByIdSensor(Long idSensor);
    boolean existsByIdSensor(Long idSensor);
}

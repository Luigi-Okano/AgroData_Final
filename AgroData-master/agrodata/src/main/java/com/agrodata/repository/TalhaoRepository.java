package com.agrodata.repository;

import com.agrodata.model.Talhao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TalhaoRepository extends JpaRepository<Talhao, Long> {
    List<Talhao> findByIdFazenda(Long idFazenda);
    List<Talhao> findByIdFazendaIn(List<Long> idFazendas);

    @Query("select coalesce(sum(t.area), 0) from Talhao t where t.idFazenda = :idFazenda")
    BigDecimal somarAreaPorFazenda(@Param("idFazenda") Long idFazenda);

    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM SENSOR WHERE id_talhao = :idTalhao)
                OR EXISTS (SELECT 1 FROM COMANDO_IRRIGACAO WHERE id_talhao = :idTalhao)
                OR EXISTS (SELECT 1 FROM CLIMA WHERE id_talhao = :idTalhao)
                OR EXISTS (SELECT 1 FROM CONSUMO_AGUA WHERE id_talhao = :idTalhao)
                OR EXISTS (SELECT 1 FROM PRODUCAO_FAZENDA WHERE id_talhao = :idTalhao)
            """, nativeQuery = true)
    boolean possuiRegistrosDependentes(@Param("idTalhao") Long idTalhao);
}

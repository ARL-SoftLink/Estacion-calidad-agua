package com.estacionagua.repo;

import com.estacionagua.model.Lectura;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LecturaRepository extends JpaRepository<Lectura, Long> {

    List<Lectura> findAllByOrderByInstanteDesc(Pageable pageable);

    List<Lectura> findByEtiquetaCapturaIsNotNull();

    @Query("select l.etiquetaCaptura, count(l) from Lectura l "
         + "where l.etiquetaCaptura is not null and l.simulada = false "
         + "group by l.etiquetaCaptura")
    List<Object[]> contarPorEtiqueta();
}

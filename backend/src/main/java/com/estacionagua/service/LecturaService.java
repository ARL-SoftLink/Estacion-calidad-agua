package com.estacionagua.service;

import com.estacionagua.ml.ClasificadorKnn;
import com.estacionagua.ml.DetectorAnomalias;
import com.estacionagua.model.Lectura;
import com.estacionagua.repo.LecturaRepository;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class LecturaService {

    public static final Set<String> ETIQUETAS = Set.of("LIMPIA", "SALADA", "TURBIA", "MUY_SUCIA");

    /** Resultado que se devuelve al nodo: estado de semaforo, etiqueta y bandera de anomalia. */
    public record Resultado(int estado, String etiqueta, boolean anomalia) {
        /** Formato de texto plano para el ESP32: "estado,ETIQUETA,anomalia". */
        public String comoTexto() {
            return estado + "," + etiqueta + "," + (anomalia ? 1 : 0);
        }
    }

    private final LecturaRepository repo;
    private final ClasificadorKnn clasificador;
    private final DetectorAnomalias detector;
    private final boolean entrenarConSimulados;
    private volatile String etiquetaActiva = null;

    public LecturaService(LecturaRepository repo,
                          @Value("${app.knn.k:5}") int k,
                          @Value("${app.anomalia.ventana:20}") int ventana,
                          @Value("${app.anomalia.umbral-z:3.5}") double umbralZ,
                          @Value("${app.anomalia.std-min-tds:5}") double sdMinTds,
                          @Value("${app.anomalia.std-min-mv:20}") double sdMinMv,
                          @Value("${app.entrenar-con-simulados:false}") boolean entrenarConSimulados) {
        this.repo = repo;
        this.clasificador = new ClasificadorKnn(k);
        this.detector = new DetectorAnomalias(ventana, umbralZ, sdMinTds, sdMinMv);
        this.entrenarConSimulados = entrenarConSimulados;
    }

    @PostConstruct
    void iniciar() {
        reentrenar();
    }

    public Resultado procesar(double t, double tds, double mv, boolean simulada) {
        String calidad = clasificador.predecir(new double[] {tds, mv});
        boolean anomalia = detector.evaluar(tds, mv);

        Lectura l = new Lectura();
        l.setInstante(Instant.now());
        l.setTemperatura(t);
        l.setTds(tds);
        l.setTurbidezMv(mv);
        l.setCalidad(calidad);
        l.setAnomalia(anomalia);
        l.setSimulada(simulada);
        l.setEtiquetaCaptura(etiquetaActiva);
        repo.save(l);

        int estado = anomalia ? 2 : semaforo(calidad);
        return new Resultado(estado, calidad, anomalia);
    }

    /** 0 = verde, 1 = amarillo, 2 = rojo. */
    static int semaforo(String calidad) {
        return switch (calidad) {
            case "LIMPIA" -> 0;
            case ClasificadorKnn.SIN_MODELO, "SALADA", "TURBIA" -> 1;
            default -> 2;
        };
    }

    public List<Lectura> ultimas(int n) {
        int tam = Math.max(1, Math.min(n, 500));
        List<Lectura> lista = new java.util.ArrayList<>(
                repo.findAllByOrderByInstanteDesc(PageRequest.of(0, tam)));
        java.util.Collections.reverse(lista);
        return lista;
    }

    /** Activa o desactiva el modo captura. "NINGUNA" o vacio lo desactiva. */
    public void fijarCaptura(String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank() || etiqueta.equals("NINGUNA")) {
            etiquetaActiva = null;
        } else if (ETIQUETAS.contains(etiqueta)) {
            etiquetaActiva = etiqueta;
        } else {
            throw new IllegalArgumentException("Etiqueta no permitida: " + etiqueta);
        }
    }

    public String etiquetaActiva() { return etiquetaActiva; }

    public int reentrenar() {
        List<Lectura> datos = repo.findByEtiquetaCapturaIsNotNull().stream()
                .filter(l -> entrenarConSimulados || !l.isSimulada())
                .toList();
        clasificador.entrenar(
                datos.stream().map(l -> new double[] {l.getTds(), l.getTurbidezMv()}).toList(),
                datos.stream().map(Lectura::getEtiquetaCaptura).toList());
        return clasificador.tamano();
    }

    public Map<String, Long> conteos() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String e : ETIQUETAS) m.put(e, 0L);
        for (Object[] fila : repo.contarPorEtiqueta()) {
            m.put((String) fila[0], (Long) fila[1]);
        }
        return m;
    }

    public int muestrasModelo() { return clasificador.tamano(); }

    public String csv() {
        StringBuilder sb = new StringBuilder("instante,temperatura,tds,turbidez_mv,etiqueta,simulada\n");
        for (Lectura l : repo.findByEtiquetaCapturaIsNotNull()) {
            sb.append(l.getInstante()).append(',')
              .append(l.getTemperatura()).append(',')
              .append(l.getTds()).append(',')
              .append(l.getTurbidezMv()).append(',')
              .append(l.getEtiquetaCaptura()).append(',')
              .append(l.isSimulada()).append('\n');
        }
        return sb.toString();
    }
}

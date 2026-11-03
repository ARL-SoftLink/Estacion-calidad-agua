package com.estacionagua.ml;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Detector de anomalias estadistico (puntaje z sobre una ventana movil), en Java puro.
 * Aprende la linea base reciente de cada variable y alerta ante desviaciones bruscas.
 */
public class DetectorAnomalias {

    private static final int MIN_HISTORIAL = 10;

    private final int ventana;
    private final double umbralZ;
    private final double[] desvMin;
    private final Deque<double[]> historial = new ArrayDeque<>();

    /**
     * @param ventana  numero de lecturas recientes que forman la linea base
     * @param umbralZ  cuantas desviaciones estandar se toleran
     * @param desvMin  desviacion minima por variable (evita falsas alarmas con sensores muy estables)
     */
    public DetectorAnomalias(int ventana, double umbralZ, double... desvMin) {
        if (ventana < MIN_HISTORIAL) throw new IllegalArgumentException("ventana demasiado pequena");
        this.ventana = ventana;
        this.umbralZ = umbralZ;
        this.desvMin = desvMin.clone();
    }

    /** Evalua una lectura contra la linea base y luego la incorpora al historial. */
    public synchronized boolean evaluar(double... valores) {
        boolean anomalia = false;
        if (historial.size() >= MIN_HISTORIAL) {
            int d = valores.length;
            for (int j = 0; j < d; j++) {
                double suma = 0;
                for (double[] h : historial) suma += h[j];
                double media = suma / historial.size();
                double var = 0;
                for (double[] h : historial) var += (h[j] - media) * (h[j] - media);
                double sd = Math.sqrt(var / historial.size());
                double z = Math.abs(valores[j] - media) / Math.max(sd, desvMin[j]);
                if (z > umbralZ) anomalia = true;
            }
        }
        historial.addLast(valores.clone());
        while (historial.size() > ventana) historial.removeFirst();
        return anomalia;
    }

    public synchronized void reiniciar() { historial.clear(); }
}

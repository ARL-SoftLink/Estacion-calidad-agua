package com.estacionagua.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clasificador k vecinos mas cercanos (k-NN), implementado en Java puro, sin dependencias.
 * Estandariza cada caracteristica (z-score) y vota ponderando por inverso de la distancia.
 */
public class ClasificadorKnn {

    public static final String SIN_MODELO = "SIN_MODELO";
    private static final int MIN_MUESTRAS = 5;

    private final int k;
    private double[][] x = new double[0][];
    private String[] y = new String[0];
    private double[] media = new double[0];
    private double[] desv = new double[0];
    private boolean listo = false;

    public ClasificadorKnn(int k) {
        if (k < 1) throw new IllegalArgumentException("k debe ser >= 1");
        this.k = k;
    }

    /** Entrena (reemplaza el modelo anterior). Requiere al menos 5 muestras y 2 clases distintas. */
    public synchronized void entrenar(List<double[]> datos, List<String> etiquetas) {
        if (datos.size() != etiquetas.size()) {
            throw new IllegalArgumentException("datos y etiquetas deben tener igual tamano");
        }
        Set<String> clases = new HashSet<>(etiquetas);
        if (datos.size() < MIN_MUESTRAS || clases.size() < 2) {
            listo = false;
            x = new double[0][];
            y = new String[0];
            return;
        }
        int n = datos.size();
        int d = datos.get(0).length;
        media = new double[d];
        desv = new double[d];
        for (double[] fila : datos) {
            for (int j = 0; j < d; j++) media[j] += fila[j];
        }
        for (int j = 0; j < d; j++) media[j] /= n;
        for (double[] fila : datos) {
            for (int j = 0; j < d; j++) desv[j] += (fila[j] - media[j]) * (fila[j] - media[j]);
        }
        for (int j = 0; j < d; j++) {
            desv[j] = Math.sqrt(desv[j] / n);
            if (desv[j] < 1e-9) desv[j] = 1.0;
        }
        x = new double[n][d];
        y = new String[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < d; j++) x[i][j] = (datos.get(i)[j] - media[j]) / desv[j];
            y[i] = etiquetas.get(i);
        }
        listo = true;
    }

    public synchronized boolean estaEntrenado() { return listo; }

    public synchronized int tamano() { return listo ? x.length : 0; }

    /** Devuelve la etiqueta predicha o SIN_MODELO si aun no hay datos suficientes. */
    public synchronized String predecir(double[] valores) {
        if (!listo) return SIN_MODELO;
        int d = media.length;
        double[] q = new double[d];
        for (int j = 0; j < d; j++) q[j] = (valores[j] - media[j]) / desv[j];

        int n = x.length;
        double[] dist = new double[n];
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            for (int j = 0; j < d; j++) {
                double diff = x[i][j] - q[j];
                s += diff * diff;
            }
            dist[i] = Math.sqrt(s);
            idx[i] = i;
        }
        Arrays.sort(idx, (a, b) -> Double.compare(dist[a], dist[b]));

        int kEf = Math.min(k, n);
        Map<String, Double> votos = new HashMap<>();
        for (int i = 0; i < kEf; i++) {
            double peso = 1.0 / (dist[idx[i]] + 1e-6);
            votos.merge(y[idx[i]], peso, Double::sum);
        }
        String mejor = null;
        double max = -1;
        for (Map.Entry<String, Double> e : votos.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                mejor = e.getKey();
            }
        }
        return mejor;
    }

    /** Utilidad de apoyo: lista de clases conocidas por el modelo. */
    public synchronized List<String> clases() {
        return new ArrayList<>(new HashSet<>(Arrays.asList(y)));
    }
}

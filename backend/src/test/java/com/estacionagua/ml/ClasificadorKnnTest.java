package com.estacionagua.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ClasificadorKnnTest {

    private static void agregar(List<double[]> x, List<String> y, Random r,
                                String et, double tds, double mv, int n) {
        for (int i = 0; i < n; i++) {
            x.add(new double[] {tds + r.nextGaussian() * 8, mv + r.nextGaussian() * 30});
            y.add(et);
        }
    }

    @Test
    void sinDatosDevuelveSinModelo() {
        ClasificadorKnn c = new ClasificadorKnn(5);
        assertFalse(c.estaEntrenado());
        assertEquals(ClasificadorKnn.SIN_MODELO, c.predecir(new double[] {100, 4000}));
    }

    @Test
    void unaSolaClaseNoEntrena() {
        ClasificadorKnn c = new ClasificadorKnn(3);
        List<double[]> x = new ArrayList<>();
        List<String> y = new ArrayList<>();
        agregar(x, y, new Random(1), "LIMPIA", 120, 4100, 10);
        c.entrenar(x, y);
        assertFalse(c.estaEntrenado());
    }

    @Test
    void separaClasesBienDefinidas() {
        Random r = new Random(42);
        List<double[]> x = new ArrayList<>();
        List<String> y = new ArrayList<>();
        agregar(x, y, r, "LIMPIA", 120, 4100, 20);
        agregar(x, y, r, "SALADA", 900, 4000, 20);
        agregar(x, y, r, "TURBIA", 200, 2800, 20);
        ClasificadorKnn c = new ClasificadorKnn(5);
        c.entrenar(x, y);
        assertTrue(c.estaEntrenado());
        assertEquals("LIMPIA", c.predecir(new double[] {125, 4090}));
        assertEquals("SALADA", c.predecir(new double[] {890, 4010}));
        assertEquals("TURBIA", c.predecir(new double[] {210, 2790}));
    }
}

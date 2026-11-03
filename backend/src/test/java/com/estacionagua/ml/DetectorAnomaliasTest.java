package com.estacionagua.ml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class DetectorAnomaliasTest {

    @Test
    void lecturasEstablesNoGeneranAlerta() {
        DetectorAnomalias d = new DetectorAnomalias(20, 3.5, 5, 20);
        Random r = new Random(7);
        for (int i = 0; i < 60; i++) {
            assertFalse(d.evaluar(120 + r.nextGaussian() * 2, 4100 + r.nextGaussian() * 10), "iteracion " + i);
        }
    }

    @Test
    void saltoBruscoGeneraAlerta() {
        DetectorAnomalias d = new DetectorAnomalias(20, 3.5, 5, 20);
        Random r = new Random(7);
        for (int i = 0; i < 30; i++) d.evaluar(120 + r.nextGaussian() * 2, 4100 + r.nextGaussian() * 10);
        assertTrue(d.evaluar(900, 4100));
    }

    @Test
    void sinHistorialSuficienteNoAlerta() {
        DetectorAnomalias d = new DetectorAnomalias(20, 3.5, 5, 20);
        assertFalse(d.evaluar(120, 4100));
        assertFalse(d.evaluar(900, 2000));
    }
}

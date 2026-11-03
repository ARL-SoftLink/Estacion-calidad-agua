package com.estacionagua.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo JSON que envia el nodo: {"t":24.50,"tds":312.0,"mv":2870}. El campo "sim" es opcional. */
public record LecturaRequest(
        @JsonProperty("t") double t,
        @JsonProperty("tds") double tds,
        @JsonProperty("mv") double mv,
        @JsonProperty("sim") boolean sim) {

    /** Comprueba rangos fisicamente razonables y descarta NaN/infinitos. */
    public boolean esValida() {
        return enRango(t, -10, 110) && enRango(tds, 0, 10000) && enRango(mv, 0, 5000);
    }

    private static boolean enRango(double v, double min, double max) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v >= min && v <= max;
    }
}

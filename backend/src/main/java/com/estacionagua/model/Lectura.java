package com.estacionagua.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Una lectura enviada por el nodo de sensores. */
@Entity
@Table(name = "lecturas")
public class Lectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant instante;

    private double temperatura;   // grados Celsius
    private double tds;           // partes por millon (ppm), compensado por temperatura
    private double turbidezMv;    // milivoltios crudos del modulo de turbidez

    /** Etiqueta manual con la que se capturo la muestra (null si no estaba en modo captura). */
    @Column(length = 24)
    private String etiquetaCaptura;

    /** Clasificacion automatica de la lectura. */
    @Column(length = 24)
    private String calidad;

    private boolean anomalia;

    /** true si la lectura proviene del simulador y no de sensores reales. */
    private boolean simulada;

    public Lectura() { }

    public Long getId() { return id; }
    public Instant getInstante() { return instante; }
    public void setInstante(Instant instante) { this.instante = instante; }
    public double getTemperatura() { return temperatura; }
    public void setTemperatura(double temperatura) { this.temperatura = temperatura; }
    public double getTds() { return tds; }
    public void setTds(double tds) { this.tds = tds; }
    public double getTurbidezMv() { return turbidezMv; }
    public void setTurbidezMv(double turbidezMv) { this.turbidezMv = turbidezMv; }
    public String getEtiquetaCaptura() { return etiquetaCaptura; }
    public void setEtiquetaCaptura(String etiquetaCaptura) { this.etiquetaCaptura = etiquetaCaptura; }
    public String getCalidad() { return calidad; }
    public void setCalidad(String calidad) { this.calidad = calidad; }
    public boolean isAnomalia() { return anomalia; }
    public void setAnomalia(boolean anomalia) { this.anomalia = anomalia; }
    public boolean isSimulada() { return simulada; }
    public void setSimulada(boolean simulada) { this.simulada = simulada; }
}

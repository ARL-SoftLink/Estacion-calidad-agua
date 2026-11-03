package com.estacionagua.web;

import com.estacionagua.model.Lectura;
import com.estacionagua.service.LecturaService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LecturaController {

    private final LecturaService servicio;
    private final String apiKey;

    public LecturaController(LecturaService servicio, @Value("${app.api-key}") String apiKey) {
        this.servicio = servicio;
        this.apiKey = apiKey;
    }

    /** Endpoint del nodo (ESP32). Responde texto plano: "estado,ETIQUETA,anomalia". */
    @PostMapping(value = "/lecturas", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> recibir(
            @RequestHeader(value = "X-API-KEY", required = false) String clave,
            @RequestBody LecturaRequest req) {
        if (clave == null || !MessageDigest.isEqual(
                clave.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("clave invalida");
        }
        if (!req.esValida()) {
            return ResponseEntity.badRequest().body("valores fuera de rango");
        }
        LecturaService.Resultado r = servicio.procesar(req.t(), req.tds(), req.mv(), req.sim());
        return ResponseEntity.ok(r.comoTexto());
    }

    @GetMapping("/lecturas/ultimas")
    public List<Lectura> ultimas(@RequestParam(defaultValue = "60") int n) {
        return servicio.ultimas(n);
    }

    @GetMapping("/captura")
    public Map<String, Object> estadoCaptura() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("etiquetaActiva", servicio.etiquetaActiva() == null ? "NINGUNA" : servicio.etiquetaActiva());
        m.put("conteos", servicio.conteos());
        m.put("muestrasModelo", servicio.muestrasModelo());
        return m;
    }

    @PostMapping("/captura")
    public ResponseEntity<Map<String, Object>> fijarCaptura(@RequestParam String etiqueta) {
        try {
            servicio.fijarCaptura(etiqueta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(estadoCaptura());
    }

    @PostMapping("/modelo/reentrenar")
    public Map<String, Object> reentrenar() {
        int n = servicio.reentrenar();
        return Map.of("muestrasModelo", n, "entrenado", n > 0);
    }

    @GetMapping(value = "/datos.csv", produces = "text/csv")
    public ResponseEntity<String> datos() {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=muestras_etiquetadas.csv")
                .body(servicio.csv());
    }
}

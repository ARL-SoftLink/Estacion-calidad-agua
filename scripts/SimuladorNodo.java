import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Random;

/**
 * Simulador del nodo de sensores, para probar el backend y el panel SIN hardware.
 *
 * Uso (JDK 11 o superior, sin compilar):
 *   java scripts/SimuladorNodo.java [urlBase] [apiKey] [--captura]
 * Ejemplo:
 *   java scripts/SimuladorNodo.java http://localhost:8080 clave-dev-123 --captura
 *
 * IMPORTANTE: los valores son INVENTADOS para desarrollo. Todas las lecturas se envian
 * marcadas como simuladas ("sim":true) y el panel muestra un aviso. Nunca las presente
 * como mediciones reales.
 */
public class SimuladorNodo {

    record Escenario(String etiqueta, double tds, double mv) { }

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "http://localhost:8080";
        String clave = args.length > 1 ? args[1] : "clave-dev-123";
        boolean captura = false;
        for (String a : args) if (a.equals("--captura")) captura = true;

        Escenario[] escenarios = {
            new Escenario("LIMPIA", 120, 4100),
            new Escenario("SALADA", 900, 4000),
            new Escenario("TURBIA", 200, 2800),
            new Escenario("MUY_SUCIA", 700, 2300)
        };

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        Random r = new Random(11);

        for (Escenario e : escenarios) {
            if (captura) {
                post(http, base + "/api/captura?etiqueta=" + e.etiqueta(), clave, "");
            }
            System.out.println("== Escenario " + e.etiqueta() + (captura ? " (capturando)" : ""));
            for (int i = 0; i < 25; i++) {
                double t = 24.5 + r.nextGaussian() * 0.2;
                double tds = e.tds() + r.nextGaussian() * 6;
                double mv = e.mv() + r.nextGaussian() * 25;
                String json = String.format(Locale.US,
                    "{\"t\":%.2f,\"tds\":%.1f,\"mv\":%.0f,\"sim\":true}", t, Math.max(0, tds), mv);
                String resp = post(http, base + "/api/lecturas", clave, json);
                System.out.println("  " + json + "  ->  " + resp);
                Thread.sleep(700);
            }
        }
        if (captura) {
            post(http, base + "/api/captura?etiqueta=NINGUNA", clave, "");
            post(http, base + "/api/modelo/reentrenar", clave, "");
            System.out.println("Captura finalizada y modelo reentrenado.");
        }
    }

    static String post(HttpClient http, String url, String clave, String cuerpo) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .header("X-API-KEY", clave)
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        return res.statusCode() + " " + res.body().replace("\n", " ");
    }
}

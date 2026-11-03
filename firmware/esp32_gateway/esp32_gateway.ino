/*
 * esp32_gateway.ino  -  Puerta de enlace Wi-Fi (ESP32 DevKit, 3.3 V)
 *
 * Qué hace:
 *   - Recibe del Arduino una línea JSON por UART2 (RX=GPIO16, TX=GPIO17, 9600 baudios).
 *   - La reenvía tal cual al backend Spring Boot (HTTP POST a /api/lecturas).
 *   - Devuelve al Arduino la respuesta del servidor con el prefijo "R,".
 *
 * Preparación en el IDE de Arduino:
 *   - Gestor de placas: instalar "esp32" de Espressif Systems; placa "ESP32 Dev Module".
 *   - Copiar secrets.example.h a secrets.h y completar los datos.
 *   - No requiere librerías externas (WiFi.h y HTTPClient.h vienen con el paquete de la placa).
 *
 * NOTA: este código no fue compilado ni probado sobre hardware por quien lo redactó.
 */
#include <WiFi.h>
#include <HTTPClient.h>
#include "secrets.h"      // define WIFI_SSID, WIFI_PASS, SERVER_URL, API_KEY

const int  PIN_RX2 = 16;   // recibe desde Arduino (a través del divisor de voltaje)
const int  PIN_TX2 = 17;   // envía a Arduino (3.3 V; el Uno lo lee como nivel alto)
HardwareSerial hub(2);

void conectarWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  Serial.print("Conectando a Wi-Fi ");
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  unsigned long t0 = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - t0 < 15000) {
    delay(300);
    Serial.print(".");
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("\nIP del ESP32: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\nNo se pudo conectar; se reintentará.");
  }
}

void reenviar(const String &json) {
  if (WiFi.status() != WL_CONNECTED) {
    hub.println("R,E");
    return;
  }
  HTTPClient http;
  http.setTimeout(2500);
  http.begin(SERVER_URL);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-API-KEY", API_KEY);
  int codigo = http.POST(json);

  if (codigo == 200) {
    String cuerpo = http.getString();
    cuerpo.trim();
    hub.print("R,");
    hub.println(cuerpo);
    Serial.println("OK  -> " + cuerpo);
  } else {
    hub.println("R,E");
    Serial.print("Error HTTP: ");
    Serial.println(codigo);
  }
  http.end();
}

void setup() {
  Serial.begin(115200);
  hub.begin(9600, SERIAL_8N1, PIN_RX2, PIN_TX2);
  hub.setTimeout(300);
  conectarWifi();
}

void loop() {
  conectarWifi();
  if (hub.available()) {
    String linea = hub.readStringUntil('\n');
    linea.trim();
    if (linea.length() > 2 && linea.charAt(0) == '{') {
      Serial.println("RX <- " + linea);
      reenviar(linea);
    }
  }
}

/*
 * arduino_hub.ino  -  Nodo de sensores (Arduino Uno o compatible, 5 V)
 *
 * Qué hace:
 *   1. Lee temperatura (DS18B20), TDS y turbidez (módulos analógicos).
 *   2. Procesa: mediana de 30 muestras + compensación de temperatura del TDS.
 *   3. Envía una línea JSON al ESP32 por UART (SoftwareSerial, 9600 baudios).
 *   4. Recibe la respuesta del servidor: "R,<estado>,<ETIQUETA>,<anomalia>".
 *   5. Muestra todo en LCD 16x2 I2C y enciende el semáforo de LEDs.
 *
 * Librerías (Administrador de bibliotecas del IDE de Arduino):
 *   - OneWire
 *   - DallasTemperature
 *   - LiquidCrystal I2C  (la de Frank de Brabander; hay varias con el mismo nombre)
 *
 * NOTA: este código no fue compilado ni probado sobre hardware por quien lo redactó.
 */
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <SoftwareSerial.h>

// ---------------- Pines ----------------
const uint8_t PIN_DS18B20   = 2;
const uint8_t PIN_ESP_RX    = 10;   // recibe desde el ESP32 (GPIO17 / TX2)
const uint8_t PIN_ESP_TX    = 11;   // envía al ESP32 (GPIO16 / RX2) A TRAVÉS DEL DIVISOR DE VOLTAJE
const uint8_t PIN_LED_VERDE = 5;
const uint8_t PIN_LED_AMAR  = 6;
const uint8_t PIN_LED_ROJO  = 7;
const uint8_t PIN_TURBIDEZ  = A0;
const uint8_t PIN_TDS       = A1;

// ---------------- Parámetros ----------------
const float         VREF                 = 5.0;     // referencia del ADC (Uno a 5 V)
const int           N_MUESTRAS           = 30;
const unsigned long PERIODO_MS           = 3000;    // un ciclo completo cada ~3 s
const unsigned long TIMEOUT_RESPUESTA_MS = 3000;
const uint8_t       LCD_DIRECCION        = 0x27;    // si no enciende, pruebe 0x3F
float               FACTOR_TDS           = 1.0;     // factor de calibración (ver docs/calibracion.md)

// ---------------- Objetos ----------------
OneWire           oneWire(PIN_DS18B20);
DallasTemperature sensorTemp(&oneWire);
SoftwareSerial    esp(PIN_ESP_RX, PIN_ESP_TX);
LiquidCrystal_I2C lcd(LCD_DIRECCION, 16, 2);

// ---------------- Estado ----------------
float temperatura = 25.0;
bool  tempOk      = false;
float tds         = 0;
float turbidezMv  = 0;

bool  hayRespuesta = false;
int   estado       = 0;
bool  anomalia     = false;
char  etiqueta[12] = "";

int bufTurb[N_MUESTRAS];
int bufTds[N_MUESTRAS];

// Mediana de un arreglo (ordena una copia in situ por inserción).
int mediana(int *a, int n) {
  for (int i = 1; i < n; i++) {
    int v = a[i];
    int j = i - 1;
    while (j >= 0 && a[j] > v) { a[j + 1] = a[j]; j--; }
    a[j + 1] = v;
  }
  return a[n / 2];
}

void leerTemperatura() {
  sensorTemp.requestTemperatures();
  float t = sensorTemp.getTempCByIndex(0);
  tempOk = (t != DEVICE_DISCONNECTED_C) && t > -55.0 && t < 125.0;
  temperatura = tempOk ? t : 25.0;   // si falla el sensor se asume 25 °C y la LCD muestra "--.-"
}

void leerAnalogicos() {
  for (int i = 0; i < N_MUESTRAS; i++) {
    bufTurb[i] = analogRead(PIN_TURBIDEZ);
    bufTds[i]  = analogRead(PIN_TDS);
    delay(20);
  }
  int adcTurb = mediana(bufTurb, N_MUESTRAS);
  int adcTds  = mediana(bufTds, N_MUESTRAS);

  turbidezMv = adcTurb * VREF * 1000.0 / 1024.0;

  // TDS: fórmula publicada por DFRobot para su módulo Gravity (verifique la de su módulo).
  float v    = adcTds * VREF / 1024.0;
  float coef = 1.0 + 0.02 * (temperatura - 25.0);   // compensación de temperatura
  float vc   = v / coef;
  float ppm  = (133.42 * vc * vc * vc - 255.86 * vc * vc + 857.39 * vc) * 0.5;
  tds = ppm * FACTOR_TDS;
  if (tds < 0) tds = 0;
}

void enviarJson() {
  while (esp.available()) esp.read();          // descarta basura anterior
  esp.print(F("{\"t\":"));   esp.print(temperatura, 2);
  esp.print(F(",\"tds\":")); esp.print(tds, 1);
  esp.print(F(",\"mv\":"));  esp.print(turbidezMv, 0);
  esp.println(F("}"));

  Serial.print(F("TX t=")); Serial.print(temperatura, 2);
  Serial.print(F(" tds=")); Serial.print(tds, 1);
  Serial.print(F(" mv="));  Serial.println(turbidezMv, 0);
}

void procesarRespuesta(char *linea) {
  hayRespuesta = false;
  if (strncmp(linea, "R,", 2) != 0) return;
  if (linea[2] == 'E') return;                  // "R,E": error del servidor o de red

  char *ctx;
  char *tok = strtok_r(linea + 2, ",", &ctx);
  if (!tok) return;
  int e = atoi(tok);

  tok = strtok_r(NULL, ",", &ctx);
  if (!tok) return;
  strncpy(etiqueta, tok, sizeof(etiqueta) - 1);
  etiqueta[sizeof(etiqueta) - 1] = '\0';

  tok = strtok_r(NULL, ",", &ctx);
  if (!tok) return;

  anomalia     = (atoi(tok) == 1);
  estado       = e;
  hayRespuesta = true;
}

void esperarRespuesta() {
  char buf[40];
  uint8_t n = 0;
  unsigned long t0 = millis();
  hayRespuesta = false;
  while (millis() - t0 < TIMEOUT_RESPUESTA_MS) {
    while (esp.available()) {
      char c = esp.read();
      if (c == '\n') {
        buf[n] = '\0';
        procesarRespuesta(buf);
        return;
      }
      if (c != '\r' && n < sizeof(buf) - 1) buf[n++] = c;
    }
  }
}

void actualizarLeds() {
  digitalWrite(PIN_LED_VERDE, hayRespuesta && estado == 0 ? HIGH : LOW);
  digitalWrite(PIN_LED_AMAR,  hayRespuesta && estado == 1 ? HIGH : LOW);
  digitalWrite(PIN_LED_ROJO,  hayRespuesta && estado >= 2 ? HIGH : LOW);
}

void mostrarLcd() {
  char tbuf[8];
  char l0[17];
  char l1[17];
  if (tempOk) dtostrf(temperatura, 4, 1, tbuf); else strcpy(tbuf, "--.-");
  snprintf(l0, sizeof(l0), "T:%sC TDS:%4d", tbuf, (int)(tds + 0.5));

  if (!hayRespuesta)  snprintf(l1, sizeof(l1), "%-16s", "Sin conexion IA");
  else if (anomalia)  snprintf(l1, sizeof(l1), "%-16s", "ALERTA ANOMALIA");
  else                snprintf(l1, sizeof(l1), "Agua: %-10s", etiqueta);

  lcd.setCursor(0, 0); lcd.print(l0);
  lcd.setCursor(0, 1); lcd.print(l1);
}

void setup() {
  Serial.begin(9600);
  esp.begin(9600);
  sensorTemp.begin();
  pinMode(PIN_LED_VERDE, OUTPUT);
  pinMode(PIN_LED_AMAR, OUTPUT);
  pinMode(PIN_LED_ROJO, OUTPUT);

  lcd.init();
  lcd.backlight();
  lcd.setCursor(0, 0); lcd.print(F("Calidad de agua"));
  lcd.setCursor(0, 1); lcd.print(F("Iniciando..."));

  // Prueba rápida de LEDs
  digitalWrite(PIN_LED_VERDE, HIGH); delay(250); digitalWrite(PIN_LED_VERDE, LOW);
  digitalWrite(PIN_LED_AMAR, HIGH);  delay(250); digitalWrite(PIN_LED_AMAR, LOW);
  digitalWrite(PIN_LED_ROJO, HIGH);  delay(250); digitalWrite(PIN_LED_ROJO, LOW);
}

void loop() {
  unsigned long inicio = millis();

  leerTemperatura();
  leerAnalogicos();
  mostrarLcd();            // muestra valores propios aunque no haya red
  enviarJson();
  esperarRespuesta();
  actualizarLeds();
  mostrarLcd();

  while (millis() - inicio < PERIODO_MS) { /* espera el resto del periodo */ }
}

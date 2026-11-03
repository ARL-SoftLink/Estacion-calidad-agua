# Cableado

Se asume un **Arduino Uno o compatible (5 V)** y un **ESP32 DevKit de 30 pines con GPIO16/17 libres**.
Si su Arduino es otro modelo o su ESP32 usa esos pines para otra función, ajuste `PIN_*` en los sketches.

## Tabla de conexiones

| Componente | Pin del componente | Conecta a |
|---|---|---|
| DS18B20 | Rojo (VCC) | Arduino 5V |
| DS18B20 | Negro (GND) | Arduino GND |
| DS18B20 | Amarillo/Blanco (DATA) | Arduino D2 **y** resistencia 4,7 kΩ hacia 5V |
| Módulo TDS | V / VCC | Arduino 5V |
| Módulo TDS | G / GND | Arduino GND |
| Módulo TDS | A / señal | Arduino A1 |
| Módulo turbidez (placa convertidora) | V / VCC | Arduino 5V |
| Módulo turbidez | G / GND | Arduino GND |
| Módulo turbidez | A / señal analógica | Arduino A0 |
| LCD 16x2 I2C | VCC | Arduino 5V |
| LCD 16x2 I2C | GND | Arduino GND |
| LCD 16x2 I2C | SDA | Arduino A4 |
| LCD 16x2 I2C | SCL | Arduino A5 |
| LED verde (+ resistencia 220 Ω) | ánodo | Arduino D5 |
| LED amarillo (+ resistencia 220 Ω) | ánodo | Arduino D6 |
| LED rojo (+ resistencia 220 Ω) | ánodo | Arduino D7 |
| LEDs | cátodos | GND |
| Arduino D11 (TX) | → divisor de voltaje → | ESP32 GPIO16 (RX2) |
| Arduino D10 (RX) | ← directo ← | ESP32 GPIO17 (TX2) |
| Arduino GND | ↔ | ESP32 GND (**obligatorio**) |

## Divisor de voltaje (Arduino TX → ESP32 RX)

El Arduino entrega 5 V y los pines del ESP32 toleran 3,3 V. Use dos resistencias:

```
Arduino D11 ──[ 1 kΩ ]──┬── ESP32 GPIO16
                        │
                     [ 2 kΩ ]        (o dos de 1 kΩ en serie)
                        │
                       GND
```

En sentido contrario (ESP32 TX → Arduino RX) basta conexión directa: 3,3 V se interpreta como nivel alto en un
Arduino Uno según su ficha técnica general; si observa pérdida de datos, use un conversor de nivel.

## Alimentación

- Alimente el Arduino y el ESP32 con **cables USB separados** (a la computadora o a un cargador/power bank) y una el GND.
- Los módulos de sensores toman 5 V del Arduino.
- Verifique en la hoja de datos de **su** módulo de turbidez y de TDS el voltaje de alimentación y el rango de la salida
  analógica; los módulos varían entre fabricantes.

## Precauciones

- Sumerja solo las sondas. Las placas convertidoras y el cableado no son resistentes al agua.
- Trabaje a bajo voltaje (USB). No use tomacorrientes cerca de los recipientes.
- Si el LCD no responde, busque su dirección I2C con un sketch "I2C scanner" (normalmente 0x27 o 0x3F).
- Guarde aquí (`hardware/cableado/`) una foto de su montaje final y un diagrama propio.

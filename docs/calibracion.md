# Calibración y captura de datos

Los sensores baratos no son instrumentos certificados. Esta guía busca que sus mediciones sean **consistentes y comparables**.

## 1. Temperatura (DS18B20)
Compare con un termómetro conocido en agua a temperatura ambiente. Si hay diferencia constante, anótela en el informe.

## 2. TDS
- Si dispone de un medidor de TDS de mano o de una solución de referencia, mida la misma muestra con ambos y calcule
  `FACTOR_TDS = valor de referencia / valor del módulo` (promedio de varias lecturas). Escriba el resultado en `FACTOR_TDS`
  de `arduino_hub.ino`.
- Si no dispone de referencia, **no afirme valores absolutos**: use los resultados como comparación relativa entre muestras.
- La fórmula del sketch es la publicada por un fabricante conocido para su módulo; su módulo puede diferir.

## 3. Turbidez
El sistema usa el voltaje en milivoltios, **sin convertir a NTU** (esa conversión varía entre módulos y no está calibrada).
Registre el valor en agua limpia como línea base. En muchos módulos el voltaje baja cuando el agua se enturbia, pero compruebe
el comportamiento del suyo.

## 4. Protocolo de captura de muestras

1. Prepare 4 recipientes: agua limpia, agua con sal, agua con tierra (turbia) y agua con tierra y sal o detergente (muy sucia).
   Anote las cantidades usadas para poder repetirlas.
2. En el panel, elija la muestra y sumerja el sensor. Espere unos 20 segundos a que se estabilice antes de capturar.
3. Capture al menos 30 lecturas por tipo. Enjuague el sensor entre muestras.
4. Repita en **tres sesiones distintas** (días u horas diferentes). Así el modelo ve variación real.
5. Pulse *Entrenar con las muestras*. Exporte el CSV y guárdelo en `data/`.

## 5. Evaluación honesta (prueba ciega)

Para no engañarse con el resultado, evalúe con lecturas que el modelo **no usó**:

1. Entrene con las sesiones 1 y 2.
2. En la sesión 3 deje la captura en **Ninguna** y sumerja el sensor en muestras cuyo tipo usted conoce.
3. Para cada muestra anote el tipo real y lo que indica el panel (mínimo 10 pruebas por tipo).
4. Calcule: `exactitud = aciertos / pruebas`. Reporte el resultado tal como salga, junto con una tabla de aciertos y errores.

## 6. Ajuste de la detección de anomalías
En `application.properties`: `app.anomalia.umbral-z` (más alto = menos alertas) y `app.anomalia.std-min-tds` / `std-min-mv`
(variación mínima que se considera ruido). Pruebe con agua estable para comprobar que no hay falsas alarmas.

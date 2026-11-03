# Maqueta y demostración para la feria

Objetivo: que un jurado entienda en 10 segundos **qué mide, qué decide y por qué importa**, y vea cambios en tiempo real.

## Concepto: "Torre de monitoreo" + mesa de muestras

```
        ┌────────────────────────────────────────────┐
        │  Póster (problema · arquitectura · QR)     │
        └────────────────────────────────────────────┘
   ┌───────────┐                              ┌───────────────┐
   │ TORRE     │      ┌────┐ ┌────┐ ┌────┐    │ Laptop/tablet │
   │ LCD 16x2  │      │ 1  │ │ 2  │ │ 3  │    │ con el panel  │
   │ ● ● ●     │      │LIMP│ │SALA│ │TURB│    │ web           │
   │ (semáforo)│      └────┘ └────┘ └────┘    └───────────────┘
   │ Arduino + │   [ vasos transparentes rotulados, sobre bandeja ]
   │ ESP32     │
   └───────────┘
   Base de cartón pluma / MDF, ~60 x 40 cm, con "río" pintado
```

## Materiales sugeridos (reutilice lo que pueda)

- Base rígida (cartón pluma o MDF) pintada como cuenca con un río; es solo ambientación.
- Torre o caja vertical para LCD y tres LEDs a la altura de la vista; Arduino y ESP32 dentro, con ventana transparente si es posible.
- Tres o cuatro vasos o frascos transparentes con etiquetas grandes: *Agua limpia*, *Agua salada*, *Agua turbia*, *Agua muy sucia*.
- Bandeja con borde y paños absorbentes para derrames.
- Póster con: problema ambiental, diagrama de arquitectura, resultado de la prueba ciega y código QR al repositorio.
- Tarjetas pequeñas: "¿Qué mide cada sensor?" y "Limitaciones del sistema".

## Seguridad

- Todo funciona a bajo voltaje por USB. Mantenga la electrónica **elevada** respecto de los recipientes y el cableado ordenado.
- Sumerja solo las sondas; no las placas convertidoras.
- Lleve extensión y cargador de respaldo.

## Guion de demostración (3 minutos)

1. **(20 s) Problema:** "Los análisis de agua son caros; con sensores económicos podemos vigilar cambios."
2. **(30 s) Qué mide:** señale los tres sensores y la ruta Arduino → ESP32 → servidor Java.
3. **(60 s) En vivo:** sumerja el sensor en agua limpia (verde en el semáforo y en el panel), luego en la salada y luego en la turbia.
   Muestre el vaso del panel cambiando de color y el LCD actualizándose.
4. **(30 s) Alerta:** con el sensor en agua limpia, añada una pizca de sal o tierra y muestre la alerta de cambio brusco.
5. **(30 s) Honestidad:** muestre su tabla de prueba ciega con la exactitud real y explique que es un comparador, no un certificador.
6. **(10 s) Cierre:** QR al repositorio.

Antes de la feria ensaye el guion al menos tres veces completas.

## Plan de contingencia

| Falla | Respuesta |
|---|---|
| No hay Wi-Fi en el evento | Use hotspot del celular o router propio (configure ambos en `secrets.h` con anticipación). |
| El servidor no responde | El LCD sigue mostrando temperatura y TDS; reinicie el backend y la base de datos. |
| Un sensor falla | Lleve un sensor de repuesto y un video corto de la demostración funcionando. |
| La clasificación se equivoca | Explíquelo con su tabla de prueba ciega; no la oculte. |
| Se acaba la batería | Lleve cargador y power bank; pruebe la duración antes. |

## Lo que debe guardar en esta carpeta
Fotos de la maqueta terminada, medidas, lista de materiales usados y una foto del montaje electrónico.

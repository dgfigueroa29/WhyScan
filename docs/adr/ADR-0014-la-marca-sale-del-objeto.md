# ADR-0014 — La marca sale del objeto que la app lee, no del nombre ni de la categoría

- **Estado:** Aceptada
- **Fecha:** 2026-08-24

## Contexto

La identidad anterior era un sistema de diseño correcto —icono adaptativo con capa monocroma,
treinta
roles de color declarados, contraste AA verificado por test— construido sobre dos elecciones que
nadie había examinado:

- **El símbolo eran cuatro esquinas de encuadre y una línea de lectura.** Ese dibujo es el icono
  `QrCodeScanner` que Material ya trae, y el que usan Google Lens, la cámara del sistema y otras
  doscientas apps de la tienda. En una ficha de Play, junto a sus competidores, un símbolo así no
  distingue: **agrupa**.
- **El color era `#2563EB`**, que es el azul por defecto de Tailwind.

Ninguna de las dos decía nada del producto. Y al unificar el nombre (Ronda 5 del roadmap) quedó
además una tercera razón para revisarlo: la marca había sido diseñada para un nombre distinto del
definitivo.

## Qué se descartó por el camino, y por qué importa

Dos direcciones se exploraron y se rechazaron **a propósito**, porque el descarte es la decisión:

- **La marca como letra** (la inicial dibujada). Es la salida cómoda y tiene un problema de fondo:
  la inicial de un nombre la puede dibujar cualquiera, y no dice nada de lo que el producto hace.
- **La marca como signo de interrogación**, jugando con el nombre. Legible y con gracia, pero un `?`
  como icono de aplicación compite con «ayuda» y «preguntas frecuentes», que es una lectura que se
  gana sola en una cuadrícula de aplicaciones.

Cerradas esas dos, queda una sola dirección honesta: mirar **el objeto que la app lee**.

## Decisión

La marca es **el módulo fugado**: un *patrón de localización* —los cuadrados anidados que toda
esquina de un QR lleva para que un lector sepa dónde empieza el código— con el anillo abierto por
una
esquina y el módulo central ya fuera, atravesando la brecha.

Tres cosas la sostienen:

1. **El patrón de localización es el átomo más reconocible de un código y casi nadie lo usa.** No es
   genérico como unas esquinas de encuadre: es específico del objeto.
2. **Rompe una regla real.** Un patrón de localización es *siempre* concéntrico y *siempre*
   cerrado —
   si no lo fuera, ningún lector encontraría el código. Uno que no lo es llama la atención sin
   gritar.
3. **Dice lo que la app hace.** El dato estaba encerrado en un dibujo que ningún humano lee; ahora
   está fuera y sirve para algo. Eso es literalmente el producto: las acciones sobre el resultado se
   derivan del **significado** del código y no de su formato.

**La paleta pasa a grafito cálido con un único acento esmeralda.** Neutros con una pizca de amarillo
—papel y tinta, no acero— porque el trabajo de esta app no es escanear sino decirte qué acabas de
escanear. Un solo acento, para que cuando aparezca signifique algo: `secondary` es el mismo verde
apagado y `tertiary` es ámbar porque tiene un trabajo **semántico** —avisar—, no de marca.

## Consecuencias

**Positivas**

- El icono deja de agrupar y empieza a distinguir, que era el problema.
- La capa monocroma de Android 13+ funciona sin depender del color: anillo en trazo, módulo en
  macizo, y la diferencia entre línea y mancha sigue separando las dos piezas.
- El tema oscuro pasa a ser la identidad principal, que es donde esta app se usa de verdad — con
  poca
  luz y sobre una imagen de cámara.

**Negativas y su gestión**

- **El icono ya no anuncia «escáner» a primera vista.** Es un cambio real y aceptado: en una tienda
  el nombre va debajo del icono, así que lo que hace falta no es explicar la categoría sino
  distinguirse dentro de ella.
- **La marca depende de una holgura de tres unidades de rejilla.** La brecha del anillo termina en
  10.4 y el módulo empieza en 13.4; en monocroma, donde ambos se pintan del mismo color, acercarlos
  funde las dos formas en una mancha. Queda escrito en las tres copias de la forma.
- **Hay tres copias de la forma** —el `ImageVector` de Compose, el primer plano del icono adaptativo
  y
  la capa monocroma— más los PNG de respaldo y el 512×512 de la ficha. Nada las mantiene
  sincronizadas salvo la nota que las tres llevan.

## Alternativas descartadas

| Alternativa                                | Motivo                                                                                                                                |
|--------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| Conservar el visor y cambiar solo el color | El problema no era el color: el símbolo es el de la categoría entera                                                                  |
| La inicial del nombre como marca           | La salida cómoda; una letra no dice nada del producto y la copia cualquiera                                                           |
| Un signo de interrogación                  | Compite con «ayuda» y «preguntas frecuentes» en una cuadrícula de aplicaciones                                                        |
| Un módulo partido por el haz               | Era la propuesta más original y trae un problema que no se puede quitar: una diagonal sobre una forma es, universalmente, «prohibido» |
| Una esquina doblada sobre el módulo        | Agradable y poco propia: la esquina doblada es el símbolo de «documento» desde hace cuarenta años                                     |

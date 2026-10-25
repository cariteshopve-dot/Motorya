# MotorYa

App Android para llevar el carro o la moto al día: gasolina y rendimiento (km por litro), mantenimiento con aviso por km o por fecha, papeles con aviso de vencimiento y gastos del mes en dólares y bolívares. Todo se guarda en el teléfono.

Hecho por lacosta.agency.

## Probar en el teléfono

Cada cambio en `main` compila un APK nuevo y lo deja en
[Última versión de prueba](../../releases/tag/prueba). Abre el link en Chrome desde el teléfono, baja `MotorYa-prueba.apk` e instálalo.

## Código

Kotlin + Jetpack Compose, Room para los datos y WorkManager para la revisión diaria de avisos (9:00). La lógica (rendimiento, mantenimiento, montos y tasas) está en `domain/` con pruebas en `app/src/test`.

## Estilo

"Tablero": fondo grafito, acento ámbar como la aguja de un tablero de noche, números en Barlow Condensed y medidor de km por litro en el inicio.

## Gratis y pago único

- Gratis: 1 vehículo, gasolina y mantenimiento con avisos.
- Pago único (2,99 USD): varios vehículos, papeles con aviso, gastos en dólares y bolívares, sin anuncios.

# MiMascota

Sistema de placas para mascotas. Cada mascota lleva un pin impreso en 3D con un chip NFC y un código QR que abren `dominio/p/{codigo}`. Quien encuentra al animal ve una ficha pública mínima y el dueño recibe un aviso con la ubicación del escaneo.

## Estructura

| Carpeta | Contenido |
| --- | --- |
| `backend/` | API REST en Java + Spring Boot (monolito modular, Spring Security + JWT, Flyway) |
| `frontend/` | Web en React (SPA servida por Nginx) |
| `infra/` | `docker-compose.yml`, configuración de Nginx y `.env.example` |
| `docs/` | Diseño del sistema y especificación de la API |

## Stack

Java + Spring Boot, PostgreSQL y React, desplegado en un VPS con Docker Compose. Cloudflare maneja DNS y HTTPS.

## Fases

1. **MVP:** login con JWT, lotes de placas, activación, ficha de la mascota, página pública y escaneo con aviso al dueño.
2. **Fase 2:** co-responsable, traspaso de titularidad, modo perdido y alertas comunitarias.
3. **Fase 3:** adopción responsable y app móvil con notificaciones push.

## Documentación

- [Diseño del sistema](docs/diseno.md)
- [API del MVP](docs/api-mvp.md)
- [Documento de diseño original (PDF)](docs/Estudio%20inicial.pdf)

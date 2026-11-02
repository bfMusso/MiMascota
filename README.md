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

## Cómo levantarlo en local

Requisitos: Docker Desktop, y para desarrollar el backend Java 21 y Maven.

```bash
cd infra
cp .env.example .env      # completá DB_PASSWORD, JWT_SECRET, ADMIN_EMAIL y ADMIN_PASSWORD
docker compose up -d      # base de datos, backend, Nginx y Mailpit
```

| Servicio | URL en tu máquina |
| --- | --- |
| Web + API (Nginx) | http://localhost:8085 |
| Backend directo | http://localhost:8090/actuator/health |
| PostgreSQL | localhost:5440 |
| Mailpit (emails de prueba) | http://localhost:8030 |

Los puertos se eligieron para no chocar con otros proyectos (se evitan 80, 1025, 3000, 5173, 5432-5434, 8000, 8025, 8080 y 8081) y se cambian en `infra/.env`. Contenedores, red y volúmenes usan el prefijo `mimascota`.

Para trabajar solo en el backend: `docker compose up -d db` y después `cd backend && mvn spring-boot:run`. Las pruebas (`mvn test`) usan esa misma base.

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

Requisitos: Docker Desktop y Java 21 (y Node 22 cuando exista el frontend). Maven no hace falta: viene con el proyecto (`backend/mvnw.cmd`).

### Primera vez

```bash
cd infra
cp .env.example .env      # en Windows: copy .env.example .env
```

Completá en `infra/.env`: `DB_PASSWORD`, `JWT_SECRET` (texto aleatorio de 32+ caracteres), `ADMIN_EMAIL` y `ADMIN_PASSWORD`. El backend lee este mismo archivo, tanto en Docker como corriendo desde VS Code.

### Día a día (modo desarrollo)

Docker solo corre la base y el correo de prueba; el backend corre en tu máquina y se reinicia solo al guardar cambios.

```bash
cd infra
docker compose up -d          # base de datos y Mailpit
cd ../backend
mvnw.cmd spring-boot:run      # en Linux/Mac: ./mvnw; o Run en VS Code sobre MiMascotaApplication
```

El frontend (cuando exista) se levanta con `npm run dev` en `frontend/`, en el puerto 5180.

### Todo en Docker (como en el servidor)

Para probar el conjunto antes de desplegar. Cada cambio de código requiere repetir el comando.

```bash
cd infra
docker compose --profile completo up -d --build
```

### URLs y puertos

| Servicio | URL en tu máquina |
| --- | --- |
| Backend | http://localhost:8090/actuator/health |
| Web + API vía Nginx (solo modo completo) | http://localhost:8085 |
| Frontend en desarrollo (próximamente) | http://localhost:5180 |
| Mailpit (emails de prueba) | http://localhost:8030 |
| PostgreSQL | localhost:5440 |

Los puertos se eligieron para no chocar con otros proyectos (se evitan 80, 1025, 3000, 5173, 5432-5434, 8000, 8025, 8080 y 8081) y se cambian en `infra/.env`. Contenedores, red y volúmenes usan el prefijo `mimascota`.

Las pruebas (`cd backend && mvnw.cmd test`) usan la base de Docker, así que tiene que estar levantada.

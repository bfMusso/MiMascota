# API del MVP

30 endpoints bajo `/api/v1`, en JSON. Versión acordada el 31/10/2026 (ver *Decisiones*).

## Convenciones

- **Auth:** `Authorization: Bearer <access>` (JWT, ~15 min). Refresh token en cookie `httpOnly; Secure; SameSite=Strict` con path `/api/v1/auth`; la app móvil lo mandará en el body.
- **Acceso:** Público (sin token), USER (dueño con email verificado), ADMIN.
- **Errores:** Problem Details (`type`, `title`, `status`, `detail`, `errors[]`). 400 validación, 401 sin token, 403 sin permiso, 404, 409 conflicto de estado, 429 rate limit.
- **Fechas:** ISO-8601 UTC. **IDs:** UUID para usuario y mascota; la placa se identifica por `codigo`.
- **Paginación:** `?page=0&size=20` → `content`, `page`, `size`, `totalElements`.

## Auth y cuenta

| Método | Ruta | Acceso | Qué hace |
| --- | --- | --- | --- |
| POST | `/auth/registro` | Público | Crea usuario y envía código de verificación. 409 si el email existe |
| POST | `/auth/verificar-email` | Público | Email + código → marca verificado y devuelve tokens |
| POST | `/auth/reenviar-verificacion` | Público | Reenvía el código (1/min, 5/hora) |
| POST | `/auth/login` | Público | Access en body, refresh en cookie. 403 si no verificó el email |
| POST | `/auth/refresh` | Cookie | Rota el refresh y devuelve access nuevo |
| POST | `/auth/logout` | Cookie | Revoca el refresh y borra la cookie |
| POST | `/auth/olvide-password` | Público | Envía código de reset. Siempre 204 |
| POST | `/auth/reset-password` | Público | Email + código + password nueva; revoca sesiones |
| GET | `/me` | USER | Datos del usuario |
| PATCH | `/me` | USER | Edita nombre, teléfono y `mostrar_contacto` |
| POST | `/me/cambiar-password` | USER | Password actual + nueva |

Códigos: 6 dígitos, 10 minutos, 5 intentos, un solo uso.

## Público: placa y escaneo

| Método | Ruta | Acceso | Qué hace |
| --- | --- | --- | --- |
| GET | `/public/placas/{codigo}` | Público | Estado de la placa y, si está ACTIVA, la ficha pública |
| POST | `/public/placas/{codigo}/escaneos` | Público | Registra escaneo con `lat`/`lng`, `mensaje` (máx. 500) y `contacto` de quien la encontró (máx. 100), todos opcionales. 202 |

Respuesta del GET según estado:

- **SIN_ACTIVAR:** `{ estado }` → la web muestra "Activá tu placa".
- **ACTIVA con mascota:** nombre, especie y `perdida` siempre; el resto (foto, raza, color, sexo, edad, `info_medica`, `observaciones`, contacto del dueño) solo si el dueño lo marcó en `campos_publicos`. Nunca email ni dirección.
- **ACTIVA sin mascota:** mensaje genérico.
- **BLOQUEADA o inexistente:** 404 indistinto.

El escaneo avisa al dueño por email: inmediato si está perdida, máximo uno por hora por placa si no. Rate limit por IP en ambos endpoints.

## Dueño: placas y mascotas

| Método | Ruta | Acceso | Qué hace |
| --- | --- | --- | --- |
| POST | `/placas/{codigo}/activar` | USER | Activa una placa SIN_ACTIVAR con mascota nueva o `mascotaId` propio. 409 si ya está activa |
| GET | `/placas` | USER | Mis placas con estado y mascota |
| PUT | `/placas/{codigo}/mascota` | USER | Reasigna la placa o la libera (`mascotaId: null`) |
| GET | `/mascotas` | USER | Mis mascotas con sus placas |
| POST | `/mascotas` | USER | Crea una mascota sin placa |
| GET | `/mascotas/{id}` | USER | Ficha completa |
| PATCH | `/mascotas/{id}` | USER | Edita la ficha, incluidos `perdida` y `campos_publicos` |
| DELETE | `/mascotas/{id}` | USER (titular) | Borra la mascota; sus placas quedan libres |
| PUT | `/mascotas/{id}/foto` | USER | Sube foto (JPG/PNG/WebP, máx. 5 MB, se redimensiona a ~800 px) |
| DELETE | `/mascotas/{id}/foto` | USER | Quita la foto |
| GET | `/mascotas/{id}/escaneos` | USER | Historial paginado con ubicación exacta |

Fotos servidas por Nginx en `/fotos/{archivo}`. Acceso a recursos ajenos → 404.

## Admin: lotes de placas

| Método | Ruta | Acceso | Qué hace |
| --- | --- | --- | --- |
| POST | `/admin/lotes` | ADMIN | Genera N códigos (máx. 500) en SIN_ACTIVAR |
| GET | `/admin/lotes` | ADMIN | Lotes con conteo por estado |
| GET | `/admin/lotes/{lote}/placas` | ADMIN | Placas del lote, paginadas |
| GET | `/admin/lotes/{lote}/export` | ADMIN | CSV con código y URL para imprimir QR |
| GET | `/admin/placas/{codigo}` | ADMIN | Detalle con dueño y mascota |
| PATCH | `/admin/placas/{codigo}` | ADMIN | Cambia estado (ej. BLOQUEADA) |

Códigos: 8 caracteres de un alfabeto sin ambiguos (sin 0/O ni 1/I/L), ~6,5 × 10^11 combinaciones.

## Cambios al modelo de datos

- Tabla nueva `refresh_token` (id, usuario_id, token_hash, expira, revocado).
- Campos nuevos: `usuario.email_verificado`, `mascota.campos_publicos`, `escaneo.contacto` y `placa.usuario_id` (la placa queda del dueño aunque se libere de la mascota).

## Decisiones

- **Verificación de email:** código de 6 dígitos (igual en web y app; mismo mecanismo que el traspaso).
- **Mensaje de quien escanea:** hasta 500 caracteres, más un contacto opcional de hasta 100.
- **Ficha pública:** el dueño elige por mascota qué campos se ven (`campos_publicos`). Nombre y especie siempre visibles.
- **Placas por mascota:** sin límite.
- **Primer admin:** por variables de entorno `ADMIN_EMAIL` y `ADMIN_PASSWORD`; se crea al arrancar si no existe.

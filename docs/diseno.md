# Diseño del sistema

Transcripción del documento de diseño original ([Estudio inicial.pdf](Estudio%20inicial.pdf), 30/10/2026).

## Visión general

Un sistema que vincula cada mascota con una placa física, para que cualquiera que la encuentre pueda ver su ficha y avisar al dueño.

- **Placa:** pin impreso en 3D (diseño propio o del Market de Bambu) con un chip NFC embebido y un código QR. Ambos apuntan a la misma URL: `dominio/p/{codigo}`.
- **NFC:** lectura por acercamiento. **QR:** respaldo para celulares sin NFC o con el NFC apagado.
- **Ficha pública:** nombre, foto, especie, información médica relevante y contacto del dueño (solo si este lo habilita).
- **Comunidad:** alertas de mascotas perdidas por zona, avistamientos y publicaciones de adopción responsable.
- **Clientes:** web en React y, más adelante, una app móvil, ambos sobre la misma API REST.

## Decisiones tecnológicas

Stack elegido: Java + Spring Boot (API REST), PostgreSQL y React, desplegado en un VPS con Docker.

| Decisión | Elección | Motivo |
| --- | --- | --- |
| Backend | Java + Spring Boot, servicios REST | Estándar en el mercado profesional; reutilizable por la web y la app móvil |
| Base de datos | PostgreSQL | Relacional, robusta, soporta consultas geográficas para alertas por radio |
| Frontend | React | SPA que consume la API |
| Autenticación | JWT: access token corto (~15 min) + refresh token | Necesario para la app móvil futura; en web el refresh va en cookie httpOnly |
| Hosting | VPS con Docker Compose | Spring Boot necesita un proceso JVM permanente |
| DNS y HTTPS | Cloudflare (plan gratuito) | HTTPS automático y protección ante ataques |
| Arquitectura del backend | Monolito modular | Microservicios no se justifican a este tamaño |

**Descartados:**

- **Hosting con cPanel:** orientado a PHP; la mayoría no permite correr Spring Boot.
- **Cloudflare Workers + D1 + R2:** más barato y sin mantenimiento, pero obliga a usar JS/TS y ata a la plataforma.
- **Supabase (plan gratis):** los proyectos se pausan tras una semana sin actividad; inaceptable para placas que deben responder siempre.

## Arquitectura y despliegue

Todo corre en un VPS con Docker Compose; Cloudflare solo maneja el DNS y el HTTPS.

```
Placa NFC/QR · Web (React) · App móvil
        │
  Cloudflare (DNS + HTTPS)
        │
┌──────────── VPS · Docker Compose ────────────┐
│ Nginx (sirve React, /api → Spring)           │
│        │                                     │
│ Spring Boot (API REST, JWT) ─────────────────┼──→ Email / Push
│    │              │                          │
│ PostgreSQL    Fotos (volumen)                │
└──────────────────────────────────────────────┘
```

Nginx sirve el build de React y deriva `/api/*` a Spring Boot, que guarda los datos en PostgreSQL y las fotos en un volumen de disco.

**Rutas:**

| Ruta | Uso |
| --- | --- |
| `/p/{codigo}` | Lo que abren el QR y el NFC: ficha pública o activación |
| `/app/...` | Panel del dueño |
| `/admin/...` | Panel de administración: lotes y grabado de placas |
| `/api/...` | API REST (web y app móvil) |

**Backend:** monolito modular con capas controller, service y repository y DTOs. Módulos: usuarios, placas, mascotas, archivos, alertas y adopción. Spring Security para JWT y Flyway para versionar la base.

**Repositorio:** uno solo con `backend/`, `frontend/` e `infra/` (docker-compose y configuración de Nginx).

**Regla clave:** las placas siempre apuntan al dominio propio, así el backend puede mudarse sin reimprimir nada.

## Modelo de datos

La placa y la mascota son tablas separadas: la placa pertenece al dueño y se puede reasignar o reutilizar editando la ficha.

| Tabla | Campos principales | Notas |
| --- | --- | --- |
| `usuario` | id, email, password_hash, nombre, telefono, mostrar_contacto, rol (USER / ADMIN), fecha_alta | |
| `placa` | codigo (PK aleatorio), estado (SIN_ACTIVAR / ACTIVA / BLOQUEADA), lote, mascota_id (nullable), fecha_alta | El código es el que va grabado en el QR y el NFC |
| `mascota` | id, nombre, especie, raza, sexo, fecha_nacimiento, color, foto, info_medica, observaciones, perdida | Si la mascota muere, el dueño edita la ficha y reutiliza la placa |
| `mascota_usuario` | mascota_id, usuario_id, rol (TITULAR / CORESPONSABLE) | Máximo 2 usuarios por mascota, controlado en el servicio |
| `invitacion` | id, mascota_id, email, token, estado, expira | Para sumar al co-responsable |
| `traspaso` | id, mascota_id, de_usuario, a_usuario, estado (PENDIENTE / ACEPTADO / RECHAZADO), expira | |
| `codigo_verificacion` | id, usuario_id, tipo (TRASPASO / RESET / VERIFICAR_EMAIL), codigo_hash, expira, intentos, usado | Código de 6 dígitos, guardado hasheado |
| `escaneo` | id, placa_codigo, fecha, lat, lng, mensaje, alerta_id (nullable) | Si hay alerta activa, cuenta como avistamiento |
| `alerta` | id, mascota_id, estado (ACTIVA / RESUELTA / VENCIDA), lat, lng, descripcion, fecha_inicio, fecha_fin | |
| `suscripcion` | id, usuario_id, especies, lat, lng, radio_km, canal | Alertas comunitarias por radio |
| `publicacion_adopcion` | id, mascota_id, estado (ABIERTA / EN_PRUEBA / CERRADA), descripcion, requisitos, zona | |
| `postulacion` | id, publicacion_id, usuario_id, mensaje, estado | |

Las fotos se guardan en disco (volumen del VPS) y la base solo almacena la ruta.

## Flujos

La misma URL `/p/{codigo}` muestra la activación o la ficha pública según el estado de la placa.

### Alta de placas (admin)

1. El admin genera un lote de códigos aleatorios en estado SIN_ACTIVAR.
2. Para cada código graba el NFC (Web NFC desde Chrome en Android), lo bloquea contra reescritura e imprime el QR.
3. Embebe el chip en el pin pausando la impresión en la capa correspondiente.

### Activación (dueño)

1. El dueño escanea la placa nueva y ve "Activá tu placa".
2. Se registra o inicia sesión y verifica su email.
3. Completa la ficha; la placa pasa a ACTIVA y él queda como TITULAR.

### Escaneo

1. Se registra el escaneo y la página pide permiso para compartir la ubicación.
2. **Mascota perdida:** aviso destacado; el dueño recibe push y email al instante con la ubicación; el escaneo cuenta como avistamiento de la alerta.
3. **Mascota no perdida:** aviso discreto al dueño, máximo uno por hora.
4. La ubicación exacta solo la ven los dueños; la comunidad ve una zona aproximada.

### Invitación de co-responsable

1. El titular ingresa el email de la otra persona.
2. Esta recibe un link con token que vence.
3. Al aceptar queda como CORESPONSABLE, si no se supera el máximo de 2.

### Traspaso de titularidad

1. El titular indica el email del receptor (cualquier usuario) y reingresa su contraseña.
2. Recibe en su correo un código de 6 dígitos (vence a los 10 minutos, máximo 5 intentos, un solo uso).
3. Ingresa el código y el receptor recibe la solicitud.
4. El receptor acepta; el titular anterior elige salir o quedar como co-responsable.

### Modo perdido y alertas comunitarias

1. El dueño marca la mascota como perdida, con zona y descripción.
2. Se avisa a los usuarios suscritos a esa especie dentro de su radio.
3. Pueden verla, compartirla o reportar un avistamiento.
4. Se cierra cuando el dueño marca "encontrada", o vence sola pasado un tiempo.

### Adopción responsable

1. El dueño publica la mascota con fotos, descripción, requisitos y zona.
2. Los interesados se postulan con un mensaje.
3. El dueño elige uno y lo suma como co-responsable durante un período de prueba.
4. Conforme, hace el traspaso y la publicación se cierra.

## Seguridad y privacidad

La ficha pública muestra lo mínimo para devolver la mascota; el resto queda detrás del login.

- **Códigos de placa aleatorios**, nunca secuenciales, para que nadie pueda recorrer todas las fichas.
- **Chip NFC bloqueado** tras grabarlo, para que nadie lo redirija a otro sitio.
- **Contraseñas y códigos de verificación** guardados hasheados; los códigos vencen, tienen intentos limitados y son de un solo uso.
- **Dirección del dueño:** nunca se muestra. Teléfono o WhatsApp solo si el usuario lo habilita.
- **Ubicación de escaneos:** se pide permiso a quien escanea; la exacta solo la ven los dueños.
- **Límites de envío:** avisos de escaneo y emails con rate limit para evitar abusos.
- **Protección de datos:** revisar las obligaciones de la Ley 18.331 de Uruguay (datos personales y ubicación).

## Fases de desarrollo

Se construye en tres fases; cada una deja el sistema usable.

1. **MVP:** login con JWT, generación y activación de placas, ficha de la mascota, página pública y escaneo con aviso al dueño.
2. **Fase 2:** co-responsable e invitaciones, traspaso con código por email, modo perdido y alertas comunitarias.
3. **Fase 3:** adopción responsable y app móvil con notificaciones push (Firebase).

Próximo paso: definir los endpoints de la API del MVP.

## Costos y mantenimiento

El costo fijo estimado ronda los US$6–8 por mes; valores aproximados, a confirmar al contratar.

| Concepto | Costo aproximado |
| --- | --- |
| VPS chico (Spring Boot + PostgreSQL + Nginx) | US$5–7 / mes |
| Dominio | US$10–15 / año |
| Cloudflare (DNS, HTTPS) | Gratis |
| Envío de emails transaccionales | Plan gratuito al inicio |
| Push en la app (Firebase) | Gratis |

**Mantenimiento:**

- [ ] Renovación automática del dominio: si vence, todas las placas dejan de funcionar.
- [ ] Backups periódicos de PostgreSQL y de la carpeta de fotos, guardados fuera del VPS.
- [ ] Monitor externo que avise si el sitio se cae.
- [ ] Actualizaciones de seguridad del sistema operativo y las imágenes Docker.
- [ ] Fotos redimensionadas antes de subirlas (~800 px) para ahorrar disco.

## Temas abiertos

- [x] Endpoints de la API del MVP: ver [api-mvp.md](api-mvp.md)
- [x] Nombre del proyecto: MiMascota
- [ ] Dominio.
- [ ] Duración del período de prueba en adopciones y vencimiento automático de alertas.
- [ ] Permisos del co-responsable frente al titular (qué puede y qué no).
- [ ] Proveedor de VPS y de envío de emails.
- [ ] Modelo de negocio: venta de placas, gratuito, suscripción.

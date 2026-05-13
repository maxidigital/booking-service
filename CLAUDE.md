# booking.service

Backend del sistema de turnos de Maxi Bottazzi (maxibottazzi.de/turnos.html).
Expone una API REST para listar y reservar turnos. No tiene base de datos propia — usa Google Calendar como storage vía `calendar.service`.

## Arquitectura

```
turnos.html / admin-turnos.html
        ↓ HTTP
  booking.service (Railway)
        ↓ HTTP
  calendar.service (Railway) → Google Calendar "Masajes"
        ↓ async HTTP
  email.service (Railway) → Mailjet → emails de confirmación
```

## Endpoints

### `GET /api/slots`
Retorna los turnos disponibles (eventos `SLOT` de las próximas 90 días).

### `GET /api/slots/all`
Retorna todos los turnos: disponibles + reservados. Usado por el panel admin.

### `POST /api/slots`
Crea un turno disponible. Llama a `calendar.service POST /slots`.
```json
{ "dateTime": "2026-05-20T10:00:00" }
```
La duración la calcula internamente con `session.duration.minutes` (default 120 min).

### `DELETE /api/slots/{id}`
Elimina un turno disponible. El `id` es el Google Calendar event ID (string). No se puede borrar un turno ya reservado.

### `POST /api/slots/{id}/book`
Reserva un turno. Llama a `calendar.service POST /slots/{id}/book` y después envía email de confirmación (async).
```json
{ "name": "Juan Pérez", "email": "juan@example.com" }
```
Retorna 409 si el turno ya fue reservado.

### `GET /health`
Spring Boot Actuator o endpoint propio según configuración.

## Variables de entorno (Railway)

| Variable | Descripción |
|---|---|
| `CORS_ALLOWED_ORIGINS` | Origins permitidos (default: `https://maxibottazzi.de,http://localhost:8000`) |
| `OWNER_EMAIL` | Email del dueño para notificaciones (default: `maxidigital@gmail.com`) |
| `EMAIL_SERVICE_URL` | URL de email.service (default: `http://underwater.railway.internal/send/internal`) |
| `CALENDAR_SERVICE_BASE_URL` | Base URL de calendar.service (default: `https://calendar-service-underwater.up.railway.app`) |
| `SESSION_DURATION_MINUTES` | Duración de cada sesión en minutos (default: `120`) |
| `GOOGLE_CALENDAR_ACCOUNT` | Nombre de cuenta en calendar.service (default: `maxi`) |
| `GOOGLE_CALENDAR_NAME` | Nombre de calendario en calendar.service (default: `masajes`) |

## Deploy en Railway

- Repo: `maxidigital/booking-service`, branch `main`
- Railway detecta el `Dockerfile` automáticamente
- URL pública: `https://booking-service.up.railway.app`

## Frontend

- **Panel admin**: `mb-web/admin-turnos.html` — gestión de slots (crear, ver, borrar)
- **Página de clientes**: `mb-web/turnos.html` — ver disponibilidad y reservar

El panel admin se sirve localmente con `python3 -m http.server 8888` desde `mb-web/`. Para que funcione en local, agregar `http://localhost:8888` a `CORS_ALLOWED_ORIGINS` en Railway.

## Estructura del código

```
src/main/java/blue/underwater/booking/
├── BookingServiceApplication.java        # Entry point (@EnableAsync para emails)
├── config/WebConfig.java                 # CORS
├── controller/SlotController.java        # Endpoints /api/slots
├── dto/
│   ├── SlotResponse.java                 # { id: String, dateTime, booked, clientName, clientEmail }
│   ├── BookSlotRequest.java              # { name, email }
│   └── CreateSlotRequest.java            # { dateTime }
└── service/
    ├── SlotService.java                  # Lógica: llama a calendar.service vía HttpClient
    └── EmailNotificationService.java     # Notificaciones async vía email.service
```

## Notas importantes

- El `id` de los slots es el Google Calendar event ID (string), no un UUID.
- No hay base de datos. Los slots viven en Google Calendar.
- Los slots disponibles son eventos con título exacto `SLOT` en el calendario configurado.
- Al reservar, `calendar.service` cambia el título a `*Masaje [nombre]` — ese cambio es la "reserva".
- El email se envía de forma asíncrona: si falla, la reserva igual es válida (el evento ya está actualizado en el calendario).

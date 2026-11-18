# Meeting Room Booking API (Java 17 + Spring Boot 3)

API REST para la gestión y reserva de salas de reuniones con detección de solapamientos (`409 Conflict`), validación estricta (`400 Bad Request`), filtrado por sala y fecha con paginación, migraciones con Flyway (H2 y PostgreSQL) y seguridad HTTP Basic para operaciones de escritura.

---

## 1. Requisitos previos y cómo ejecutarlo

### Opción A: Ejecución local con H2 (sin dependencias externas)
Requiere **Java 17+** y **Maven 3.9+**. Desde la carpeta `backend/`:

```bash
cd backend

# Ejecutar tests (unitarios + integración MockMvc + DataJpaTest)
mvn test

# Arrancar la aplicación (usa H2 en memoria en modo compatibilidad PostgreSQL + Flyway)
mvn spring-boot:run
```

- **API Base URL:** `http://localhost:8080/api/v1`
- **Swagger / OpenAPI UI:** `http://localhost:8080/swagger-ui.html`
- **H2 Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:meetingrooms`, User: `sa`, Password vacío)

### Opción B: Ejecución con PostgreSQL 16 t con el perfil `postgres` activo ejecutando automáticamente la migración `V1__init_schema.sql` de Flyway.

### Credenciales por defecto (operaciones de escritura)
Configurables mediante variables de entorno `APP_AUTH_USER` y `APP_AUTH_PASSWORD`:
- **Usuario:** `admin`
- **Contraseña:** `admin123`

---

## 2. Endpoints principales y ejemplos `curl`

| Método   | Ruta                        | Auth | Descripción                                                                                 |
|:---------|:----------------------------| :--- |:--------------------------------------------------------------------------------------------|
| `POST`   | `/api/v1/auth/login`        | Pública | Autenticación en el sistema (`username`, `password`)                                        |
| `GET`    | `/api/v1/rooms`             | Pública | Lista salas con paginación (`page`, `size`, `sort`)                                         |
| `POST`   | `/api/v1/rooms`             | Basic Auth | Crea una sala (`409` si el nombre ya existe)                                                |
| `GET`    | `/api/v1/reservations`      | Pública | Lista reservas con filtros `roomId`, `date` (`YYYY-MM-DD`), `includeCancelled` y paginación |
| `GET`    | `/api/v1/reservations/{id}` | Pública | Consulta una reserva por ID                                                                 |
| `POST`   | `/api/v1/reservations`      | Basic Auth | Crea una reserva (`409` si se solapa con una confirmada)                                    |
| `DELETE` | `/api/v1/reservations/{id}` | Basic Auth | Cancela una reserva (soft-cancel, libera el horario)                                        |

### Ejemplos rápidos

```bash
# 1. Crear una sala (requiere Basic Auth)
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name":"Sala Kepler","capacity":12,"location":"Planta 3"}'

# 2. Listar salas (sin autenticación)
curl "http://localhost:8080/api/v1/rooms?page=0&size=10"

# 3. Crear una reserva (requiere Basic Auth)
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 1,
    "responsible": "Marta Sánchez",
    "startTime": "2026-12-01T10:00:00",
    "endTime": "2026-12-01T11:30:00"
  }'

# 4. Consultar reservas por sala y fecha (sin autenticación)
curl "http://localhost:8080/api/v1/reservations?roomId=1&date=2026-12-01&page=0&size=20"

# 5. Cancelar una reserva (requiere Basic Auth)
curl -u admin:admin123 -X DELETE http://localhost:8080/api/v1/reservations/1
```

---

## 3. Decisiones de arquitectura y diseño

1. **Arquitectura en capas limpia (`controller` → `service` → `domain/repository`)**

2. **Detección de solapamientos con intervalos semiabiertos `[startTime, endTime)`**

3. **Control de concurrencia (`PESSIMISTIC_WRITE` sobre `Room`)**
    - Al crear una reserva, `RoomRepository.findByIdForUpdate(roomId)` adquiere un bloqueo pesimista de fila (`SELECT ... FOR UPDATE`) sobre la sala dentro de la transacción `@Transactional`.
    - De este modo, dos peticiones concurrentes para la misma sala se serializan limpiamente y la segunda recibe `409 Conflict` en lugar de sufrir una condición de carrera (*check-then-act*).

4. **Soft-cancel con estado `CONFIRMED` / `CANCELLED`**
    - `DELETE /api/v1/reservations/{id}` cambia el estado a `CANCELLED` y registra `cancelledAt`, conservando trazabilidad histórica y liberando inmediatamente la franja horaria en los comprobadores de solapamiento.

5. **Filtrado dinámico seguro con `JpaSpecificationExecutor`**
    - En lugar de queries JPQL con parámetros opcionales `(:date IS NULL OR ...)`, que en PostgreSQL provocan errores de tipado de parámetros `NULL`, se utiliza `ReservationSpecifications` junto con `@EntityGraph(attributePaths = "room")` para paginación eficiente en SQL.

6. **Base de datos y migraciones Flyway (`ddl-auto: validate`)**
    - El perfil por defecto usa H2 en `MODE=PostgreSQL` para arrancar sin fricción; el perfil `postgres` apunta a PostgreSQL 16 con el mismo script `V1__init_schema.sql` e índices compuestos sobre `(room_id, status, start_time, end_time)`.

---

## 4. Qué haría con más tiempo
- **Prueba en PostgreSQL. 

---

## 5. Cómo usé la IA
- **Qué le pedí:**
    - Generar la estructura inicial para proteger solo métodos de escritura, script inicial de Flyway y tests unitarios e integración con `MockMvc`.
    - Creación del README.
- **Qué acepté:**
    - El mapeo de respuestas `ApiErrorResponse` centralizado en `@RestControllerAdvice` y la creación de clases de excepciones.

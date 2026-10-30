# OficioYa

API REST para conectar contratantes con trabajadores de oficios (plomería, electricidad, etc.).
El proyecto implementa autenticación con JWT, autorización por roles, perfiles de usuario y un flujo
de solicitudes de servicio, con pruebas automatizadas de los requisitos de seguridad.

**Curso:** Diseño y Arquitectura de Software (DOSW) — Laboratorio 8: Seguridad

## Integrantes

| Nombre |
| --- |
| Juan José Rivera |
| Brian Steven Fierro |
| Santiago Gómez |
| Diego Mesa |
| Juan Esteban Laverde |

## Tecnologías

| Componente | Versión |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Security | incluido en Boot |
| Maven | wrapper incluido (`mvnw`) |
| PostgreSQL | 17 |
| H2 | solo para pruebas |
| JJWT | 0.12.6 |
| BCrypt | `BCryptPasswordEncoder` |

## Requisitos previos

- JDK 21 o superior
- Docker (para PostgreSQL) o una instancia de PostgreSQL accesible
- No hace falta instalar Maven: se usa el wrapper

## Configuración

Los secretos **no** se versionan. `.env.example` lista las variables necesarias y `.env` está en
`.gitignore`.

| Variable | Obligatoria | Descripción |
| --- | --- | --- |
| `DB_URL` | No | URL JDBC. Por defecto `jdbc:postgresql://localhost:5432/oficioya_security` |
| `DB_USER` | No | Usuario de la base. Por defecto `postgres` |
| `DB_PASSWORD` | **Sí** | Contraseña de la base de datos |
| `JWT_SECRET` | **Sí** | Clave de firma HS256, mínimo 32 bytes |
| `ADMIN_CORREO` | Solo perfil `dev` | Correo del administrador inicial |
| `ADMIN_PASSWORD` | Solo perfil `dev` | Contraseña del administrador inicial |

## Ejecución

### 1. Levantar PostgreSQL

```bash
docker compose up -d
```

### 2. Definir las variables de entorno

PowerShell:

```powershell
$env:DB_PASSWORD="<tu-clave>"
$env:JWT_SECRET="<clave-de-al-menos-32-bytes>"
```

Bash:

```bash
export DB_PASSWORD="<tu-clave>"
export JWT_SECRET="<clave-de-al-menos-32-bytes>"
```

### 3. Arrancar la aplicación

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### 4. Crear el administrador (opcional)

```powershell
$env:ADMIN_CORREO="admin@oficioya.com"
$env:ADMIN_PASSWORD="<clave-del-admin>"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Perfiles

| Perfil | Base de datos | Uso |
| --- | --- | --- |
| `default` | PostgreSQL (`DB_URL`) | Ejecución normal y ataques manuales |
| `test` | H2 en memoria | `mvn test`; no depende de PostgreSQL |
| `dev` | PostgreSQL | Agrega el `AdminSeeder` con `ADMIN_CORREO` y `ADMIN_PASSWORD` |

## Endpoints

Todas las rutas protegidas esperan `Authorization: Bearer <token>`.
Las rutas de autenticación son públicas.

### Autenticación

| Método | Ruta | Roles | Respuesta |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/registro` | Público | `201` con el usuario creado, sin contraseña ni hash |
| `POST` | `/api/v1/auth/login` | Público | `200` con `tipo: Bearer`, `accessToken` y `expiraEn` |

### Usuarios y perfiles

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/v1/usuarios/me` | Autenticado | Perfil propio, con datos de contacto |
| `GET` | `/api/v1/trabajadores` | Autenticado | Listado público de trabajadores, sin correo ni teléfono |
| `GET` | `/api/v1/trabajadores/{id}` | Autenticado | Perfil público de un trabajador; `404` si no existe |
| `GET` | `/api/v1/admin/usuarios` | `ADMIN` | Listado completo con datos de contacto |

### Solicitudes

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/v1/solicitudes` | `CONTRATANTE` | Crea una solicitud en estado `PENDIENTE` |
| `GET` | `/api/v1/solicitudes` | `CONTRATANTE`, `TRABAJADOR` | Lista solo las solicitudes donde participa |
| `GET` | `/api/v1/solicitudes/{id}` | `CONTRATANTE`, `TRABAJADOR` | Consulta una solicitud propia; `404` si es ajena |
| `PATCH` | `/api/v1/solicitudes/{id}/aceptar` | `TRABAJADOR` | Acepta una solicitud `PENDIENTE` asignada |
| `PATCH` | `/api/v1/solicitudes/{id}/rechazar` | `TRABAJADOR` | Rechaza una solicitud `PENDIENTE` asignada |
| `PATCH` | `/api/v1/solicitudes/{id}/cancelar` | `CONTRATANTE` | Cancela una solicitud `PENDIENTE` o `ACEPTADA` propia |

### Formato de error

Todas las respuestas de error usan el mismo cuerpo:

```json
{
  "timestamp": "2026-10-29T22:58:54",
  "status": 401,
  "error": "Unauthorized",
  "mensaje": "Token inválido o expirado",
  "ruta": "/api/v1/usuarios/me"
}
```

## Seguridad

- **Contraseñas:** se almacenan con BCrypt (`password_hash`). Nunca se devuelven en una respuesta.
- **Token:** JWT firmado con HS256, con `sub` (correo), `role`, `iss`, `iat` y `exp` de 30 minutos.
- **Sesiones:** sin estado (`SessionCreationPolicy.STATELESS`); no se usan cookies de sesión.
- **CSRF:** desactivado porque la API autentica con el encabezado `Authorization`, que el navegador no
  agrega automáticamente.
- **Identidad:** siempre sale del token (`Authentication.getName()`), nunca del cuerpo ni de la URL.
- **Autorización:** `@PreAuthorize` por rol y consultas que incluyen el id del participante, para que un
  recurso ajeno responda `404` y no `403`.
- **Códigos:** `400` datos inválidos, `401` sin token o token inválido/vencido, `403` rol insuficiente,
  `404` recurso inexistente o ajeno, `409` correo duplicado o transición de estado inválida.

## Pruebas

```bash
./mvnw clean verify
```

Resultado esperado: **22 pruebas, 0 fallos**.

| Clase | Pruebas | Cubre |
| --- | --- | --- |
| `AuthSeguridadTest` | 7 | PT-01 a PT-06: registro, hash BCrypt y login |
| `SecuritySeguridadTest` | 3 | PT-07 a PT-09: sin token, token alterado y token vencido |
| `PerfilesSeguridadTest` | 4 | PT-10 a PT-12: perfiles y autorización de admin |
| `SolicitudSeguridadTest` | 7 | PT-13 a PT-17: propiedad y roles en solicitudes |
| `OficioyaApplicationTests` | 1 | Carga del contexto |

Las pruebas usan el perfil `test` con H2 en memoria, así que no necesitan PostgreSQL.

### Cobertura (opcional)

```bash
./mvnw clean org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.13:report
```

El reporte queda en `target/site/jacoco/index.html` (no se versiona).

## Documentación y evidencias

| Documento | Contenido |
| --- | --- |
| [`docs/informe-seguridad.md`](docs/informe-seguridad.md) | Informe: hashing, JWT, códigos de error, identidad desde el token, CSRF, secretos |
| [`docs/owasp.md`](docs/owasp.md) | Matriz OWASP Top 10 aplicada al proyecto |
| [`docs/ataques.md`](docs/ataques.md) | Ataques manuales con resultado real y evidencia |
| [`docs/evidence/`](docs/evidence) | Capturas de soporte |
| [`docs/plan-equipo.md`](docs/plan-equipo.md) | Plan de trabajo y reparto de tareas |

## Estructura del proyecto

```
src/main/java/edu/eci/dosw/oficioya/
├── config/       SecurityConfig, AdminSeeder
├── controller/   Auth, Usuario, Trabajador, Admin y Solicitud
├── dto/          Requests, responses y ApiError
├── entity/       Usuario, Solicitud, Rol, EstadoSolicitud
├── exception/    Excepciones de dominio y ApiExceptionHandler
├── mapper/       Conversión entidad -> DTO
├── repository/   Repositorios JPA
├── security/     JwtService, JwtAuthenticationFilter y handlers 401/403
└── service/      AuthService, UsuarioService y SolicitudService
```

## Flujo de trabajo

- `main` contiene la versión entregada.
- `develop` integra el trabajo del equipo.
- Cada tarea se desarrolla en una rama `feature/*` y se integra con un pull request revisado.

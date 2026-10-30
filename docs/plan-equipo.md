# Plan de trabajo paralelo · Lab 8 Seguridad (OficioYa)

Objetivo: que las 5 personas trabajen al mismo tiempo sin bloquearse, con una regla central:
**Santiago y Diego solo tocan la capa web (controladores), pruebas de rol y documentación.
Nada de lo que ellos escriben es usado por el código de los demás.**

## Equipo y rol

| Persona | Rol | Por qué |
| --- | --- | --- |
| **Juan Esteban Laverde** | Base de persistencia + servicio de solicitudes + README | Dueño del repo y de la base común |
| **Juan José Rivera** | Registro, login y perfiles | Vertical de cuentas |
| **Brian Steven Fierro** | Setup, JWT y Spring Security | Autor del esqueleto y dueño de la seguridad |
| **Santiago Gómez** | Controladores de cuentas/perfiles + pruebas de rol + OWASP | Capa hoja, sin dependientes |
| **Diego Mesa** | `SolicitudController` + pruebas de propiedad + ataques + matriz | Capa hoja, sin dependientes |

---

## Estado actual verificado en Git (25-oct-2026)

| Rama | Commit | Autor | Contenido |
| --- | --- | --- | --- |
| `main` | `ab69c46` | JElaverdem | Solo `README.md` y `.gitignore` |
| `develop` | `ab69c46` | JElaverdem | Igual que `main`, sin merges |
| `feature/setup` | `3345b21` | Brian Fierro | Esqueleto del proyecto, **sin mergear** |

### Ya está hecho (no repetir en la lista de tareas)

- Proyecto Spring Boot **4.1.2 / Java 21** generado con Maven Wrapper.
- Dependencias ya declaradas: webmvc, JPA, Validation, PostgreSQL, Security, jjwt 0.12.6,
  starters de pruebas y H2.
- Paquetes `edu.eci.dosw.oficioya.*` creados (todavía con `PLACEHOLDER.txt`).
- `application.properties` con `DB_URL`, `DB_USER`, `DB_PASSWORD`, `app.jwt.secret`,
  `app.jwt.issuer` y `app.jwt.ttl-minutes`.
- `.gitignore` base con `target/`, `logs/` y `.env`.
- **Verificado:** `mvn -DskipTests compile` pasa con el JDK 26 local.

### Pendiente del Bloque 0

- Mergear `feature/setup` a `develop` (hoy `develop` sigue en el commit inicial).
- Corregir `.gitignore`: la regla `.env*` también bloquea `.env.example` → agregar `!.env.example`.
- Crear `.env.example` (sin valores) y un `.env` local (ignorado por Git).
- Levantar PostgreSQL con Docker y crear la base `oficioya_security`.
- Crear `src/test/resources/application-test.properties` con H2.
- Ordenar `pom.xml` (dependencias jjwt juntas y quitar starters de prueba redundantes).
- **Verificado:** `mvn test` falla porque no existe perfil `test` ni base de datos.

> De las 42 tareas, **ninguna está cerrada**: lo que existe es el andamio del Bloque 0 en una rama
> sin mergear. Las tareas de "crear proyecto", "agregar dependencias" y "crear paquetes" ya no
> aparecen porque están cubiertas por `feature/setup`.

---

## 0. Reglas de oro (contrato congelado)

| Archivo / paquete | Dueño | Nadie más lo edita |
| --- | --- | --- |
| `entity/**`, `repository/**` | Juan Esteban | ✅ |
| `dto/**` de auth y usuarios | Juan José | ✅ |
| `dto/**` de solicitudes | Brian | ✅ |
| `service/**` de auth y usuarios | Juan José | ✅ |
| `service/**` de solicitudes | Juan Esteban | ✅ |
| `security/**` | Brian | ✅ |
| `config/**` | Brian (excepto `AdminSeeder`, de Santiago) | ✅ |
| `exception/**` (ApiError y excepciones) | Juan Esteban | ✅ |
| `exception/**` (advice) | Juan José | ✅ |
| `controller/**` | Santiago (cuentas/perfiles) y Diego (solicitudes) | ✅ |
| `pom.xml`, `application.properties`, `.env.example` | Brian | ✅ |
| `application-test.properties` | Juan Esteban | ✅ |
| `docs/**` | Santiago y Diego (archivos separados) | ✅ |

Reglas para Santiago y Diego:

1. Consumen los servicios y DTOs congelados; **no los modifican**.
2. Si necesitan un campo nuevo o un método nuevo, lo piden por el chat/PR; no lo agregan por su cuenta.
3. No tocan `SecurityConfig`, `pom.xml` ni entidades.
4. No devuelven entidades JPA: siempre DTOs.
5. No leen el id del usuario desde el body ni la URL: usan `Authentication auth` y `auth.getName()`.
6. No ponen `try/catch`: dejan que las excepciones lleguen al `@RestControllerAdvice`.
7. No agregan `permitAll()`.

---

## 1. Fase 0 · base común (bloqueante, 60 min)

| Tarea | Dueño | Archivos | Termina |
| --- | --- | --- | --- |
| Entidades, enums y repositorios | Juan Esteban | `entity/Usuario`, `entity/Solicitud`, `entity/Rol`, `entity/EstadoSolicitud`, `repository/*` | 0:50 |
| `ApiError` y excepciones propias | Juan Esteban | `dto/ApiError`, `exception/*` | 0:50 |
| DTOs de auth y usuarios | Juan José | `dto/*` | 0:50 |
| DTOs de solicitudes | Brian | `dto/Solicitud*` | 0:50 |
| Servicios `AuthService` y `UsuarioService` (clases `@Service`) | Juan José | `service/*` | 0:40 |
| Servicio `SolicitudService` (clase `@Service`) | Brian | `service/SolicitudService` | 0:40 |
| `SecurityConfig` base + `PasswordEncoder` | Brian | `config/SecurityConfig` | 0:50 |
| `JwtService` completo | Brian | `security/JwtService` | 0:50 |
| Perfil `test` con H2 | Juan Esteban | `src/test/resources/application-test.properties` | 0:50 |
| Esqueletos de controladores de cuentas/perfiles | Santiago | `controller/AuthController`, `UsuarioController`, `TrabajadorController`, `AdminController` | 1:00 |
| Esqueleto de `SolicitudController` | Diego | `controller/SolicitudController` | 1:00 |
| Docs, `.http` y Postman | Santiago y Diego | `docs/**` | 1:00 |

### Contrato congelado (firmas exactas)

```java
public interface AuthService {
    RegistroResponse registrar(RegistroRequest request);
    LoginResponse login(LoginRequest request);
}

public interface UsuarioService {
    UsuarioMeResponse me(String correo);
    List<PerfilPublicoResponse> listarTrabajadores();
    PerfilPublicoResponse obtenerTrabajador(Long id);
    List<PerfilAdminResponse> listarParaAdmin();
}

public interface SolicitudService {
    SolicitudResponse crear(SolicitudRequest request, String correoAutenticado);
    List<SolicitudResponse> listar(String correoAutenticado);
    SolicitudResponse obtener(Long id, String correoAutenticado);
    SolicitudResponse aceptar(Long id, String correoAutenticado);
    SolicitudResponse rechazar(Long id, String correoAutenticado);
    SolicitudResponse cancelar(Long id, String correoAutenticado);
}
```

DTOs (nombres y campos):

```java
public record RegistroRequest(@NotBlank String nombre, @NotBlank @Email String correo,
        @NotBlank String telefono, @NotBlank @Size(min = 8) String password,
        @NotNull Rol rol, String oficioPrincipal, String zona) {}

public record RegistroResponse(Long id, String nombre, String correo, Rol rol) {}

public record LoginRequest(@NotBlank String correo, @NotBlank String password) {}

public record LoginResponse(String tipo, String accessToken, long expiraEn) {}

public record UsuarioMeResponse(Long id, String nombre, String correo, String telefono,
        Rol rol, String oficioPrincipal, String zona) {}

public record PerfilPublicoResponse(Long id, String nombre, String oficioPrincipal, String zona) {}

public record PerfilAdminResponse(Long id, String nombre, String correo, String telefono, Rol rol) {}

public record SolicitudRequest(@NotNull Long trabajadorId, @NotBlank String descripcion) {}

public record SolicitudResponse(Long id, Long contratanteId, Long trabajadorId,
        String descripcion, EstadoSolicitud estado, LocalDateTime fechaCreacion) {}
```

Decisión congelada: `telefono` en `Usuario` queda **nullable en la entidad** y obligatorio en
`RegistroRequest` (`@NotBlank`), para que el `ADMIN` sembrado no tenga que inventar un teléfono.

---

## 2. Asignación por persona

### Juan Esteban Laverde · base + servicio de solicitudes

**Ramas:** `feature/persistence`, `feature/requests-core`, `feature/docs` (solo README)

| Qué | Dónde |
| --- | --- |
| Entidades, enums, repositorios | `entity/**`, `repository/**` |
| `ApiError` y excepciones propias | `dto/ApiError`, `exception/**` |
| Perfil `test` con H2 | `application-test.properties` |
| Contenedor PostgreSQL y `.env` local | Docker + `.env` |
| `SolicitudService` (propiedad + estados) | `service/SolicitudService` |
| README | `README.md` |
| Apoyo a PT-14 a PT-16 | `src/test/**` |

**Depende de:** `SolicitudService` (Brian) para la firma; nada para la base.
**Bloquea a:** todos (fase 0) y a Diego (servicio de solicitudes).

### Juan José Rivera · registro, login y perfiles

**Ramas:** `feature/auth-registration`, `feature/auth-login`

| Qué | Dónde |
| --- | --- |
| DTOs de auth y usuarios | `dto/**` |
| `AuthService` (registro y login) | `service/AuthService` |
| `UsuarioService` (`/me`, trabajadores, admin) | `service/UsuarioService` |
| Mappers de usuarios | `mapper/**` |
| Advice completo (400/401/403/404/409) | `exception/ApiExceptionHandler` |
| Pruebas PT-01 a PT-06 | `src/test/**` |

**Depende de:** entidades/repos y `ApiError` (Juan Esteban); `JwtService` (Brian) para el login.
**Bloquea a:** Santiago (controladores de auth/perfiles).

### Brian Steven Fierro · setup, JWT y Spring Security

**Ramas:** `feature/setup`, `feature/jwt`, `feature/security-config`

| Qué | Dónde |
| --- | --- |
| Mergear setup, `.gitignore`, `.env.example`, orden del `pom` | raíz |
| DTOs de solicitudes | `dto/Solicitud*` |
| `SolicitudService` (clase `@Service`) | `service/SolicitudService` |
| `SecurityConfig` base y final | `config/SecurityConfig` |
| `JwtService` | `security/JwtService` |
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter` |
| Handlers `401` y `403` | `security/Json*` |
| Pruebas PT-07 a PT-09 | `src/test/**` |

**Depende de:** entidades/repositorios (Juan Esteban) para el filtro.
**Bloquea a:** Juan José (login) y Diego (implementación del servicio de solicitudes ya está en Juan Esteban).
**Meta:** publicar el PR de `JwtService` a la 1:30 máximo.

### Santiago Gómez · capa web de cuentas/perfiles + documentación

**Ramas:** `feature/web-accounts`, `feature/profiles-admin`, `feature/docs`

| Qué | Dónde |
| --- | --- |
| `POST /auth/registro`, `POST /auth/login` | `controller/AuthController` |
| `GET /usuarios/me` | `controller/UsuarioController` |
| `GET /trabajadores`, `GET /trabajadores/{id}` | `controller/TrabajadorController` |
| `GET /admin/usuarios` + `@PreAuthorize("hasRole('ADMIN')")` | `controller/AdminController` |
| Siembra del `ADMIN` en perfil `dev` | `config/AdminSeeder` |
| Pruebas PT-10 a PT-12 | `src/test/**` |
| Ataques 3, 5 y 6 + evidencias | `docs/ataques.md`, `docs/evidence/**` |
| Matriz OWASP + preguntas 1 a 3 | `docs/owasp.md`, `docs/informe-seguridad.md` |

**Depende de:** solo DTOs congelados (0:50) y servicios de Juan José (integración 2:30).
**Nadie depende de Santiago:** los controladores son la capa más externa.

### Diego Mesa · capa web de solicitudes + ataques y documentación

**Ramas:** `feature/requests-api`, `feature/docs`

| Qué | Dónde |
| --- | --- |
| Los 6 endpoints de `/solicitudes` | `controller/SolicitudController` |
| `@PreAuthorize` por rol (contratante/trabajador; `ADMIN` fuera) | `controller/SolicitudController` |
| Pruebas PT-13, PT-14, PT-15, PT-16 y PT-17 | `src/test/**` |
| Ataques 1, 2, 4 y 7 | `docs/ataques.md` |
| `.http` y colección Postman | `docs/http/oficioya.http` |
| Matriz de acceso + preguntas 4 a 6 | `docs/matriz-acceso.md`, `docs/informe-seguridad.md` |

**Depende de:** el contrato de `SolicitudService` congelado (0:40) y su implementación (Juan Esteban, 2:30).
**Nadie depende de Diego:** igual que Santiago, su código es hoja del grafo de dependencias.

---

## 3. Quién escribe cada endpoint

| Endpoint | Controlador | Servicio | Prueba |
| --- | --- | --- | --- |
| `POST /auth/registro` | Santiago | Juan José | PT-01 a PT-04 |
| `POST /auth/login` | Santiago | Juan José | PT-05, PT-06 |
| `GET /usuarios/me` | Santiago | Juan José | integración |
| `GET /trabajadores` | Santiago | Juan José | PT-10 |
| `GET /trabajadores/{id}` | Santiago | Juan José | PT-10 |
| `GET /admin/usuarios` | Santiago | Juan José | PT-11, PT-12 |
| `POST /solicitudes` | Diego | Juan Esteban | PT-13 |
| `GET /solicitudes` | Diego | Juan Esteban | PT-14, PT-17 |
| `GET /solicitudes/{id}` | Diego | Juan Esteban | PT-15 |
| `PATCH /solicitudes/{id}/aceptar` | Diego | Juan Esteban | PT-16 |
| `PATCH /solicitudes/{id}/rechazar` | Diego | Juan Esteban | PT-16 |
| `PATCH /solicitudes/{id}/cancelar` | Diego | Juan Esteban | PT-15 |

---

## 4. Pruebas

| ID | Prueba | Dueño | Cuándo corre |
| --- | --- | --- | --- |
| PT-01 | Registro válido `201` sin hash | Juan José | 2:30 |
| PT-02 | Correo repetido `409` | Juan José | 2:30 |
| PT-03 | Rol `ADMIN` o clave corta `400` | Juan José | 2:30 |
| PT-04 | Hash BCrypt distinto del texto | Juan José | 2:30 |
| PT-05 | Login correcto `200` + token | Juan José | 2:30 |
| PT-06 | Login fallido `401` mismo mensaje | Juan José | 2:30 |
| PT-07 | Sin token `401` con `ApiError` | Brian | 2:30 |
| PT-08 | Token alterado `401` | Brian | 2:30 |
| PT-09 | Token vencido `401` | Brian | 2:30 |
| PT-10 | Perfil público sin correo/teléfono | Santiago | 2:30 |
| PT-11 | `CONTRATANTE` en `/admin/usuarios` → `403` | Santiago | 2:30 |
| PT-12 | `ADMIN` en `/admin/usuarios` → `200` con contacto | Santiago | 2:30 |
| PT-13 | `TRABAJADOR` crea solicitud → `403` | Diego | 2:30 |
| PT-14 | Contratante solo ve las propias | Diego | 2:30 |
| PT-15 | Solicitud ajena → `404` sin cambios | Diego | 2:30 |
| PT-16 | Aceptar no asignado `404` / contratante `403` | Diego | 2:30 |
| PT-17 | `ADMIN` en `/solicitudes` → `403` | Diego | 2:30 |

---

## 5. Cronograma (5 horas)

| Hora | Juan Esteban | Juan José | Brian | Santiago | Diego |
| --- | --- | --- | --- | --- | --- |
| 0:00–0:20 | Congela entidades y repos | Congela DTOs de auth | Congela DTOs de solicitudes y JWT | Lee contrato, arma docs | Arma `.http` |
| 0:20–1:00 | Entidades, repos, `ApiError`, H2 | DTOs, servicios, arranca auth | Setup, `SecurityConfig`, `JwtService` | Esqueletos de controladores | Esqueleto de `SolicitudController` |
| 1:00–1:15 | PR base | Rebase | PR setup | Rebase | Rebase |
| 1:15–2:30 | `SolicitudService` + `.env`/Docker | Auth + perfiles + PT-01..06 | Filtro + handlers + PT-07..09 | 4 controladores + `AdminSeeder` | `SolicitudController` + PT-13/14/15/16/17 |
| 2:30–3:00 | PR requests-core | PR auth | PR security | PR web | PR requests-api |
| 3:00–3:20 | Corrige | Corrige | Corrige | Corrige | Corrige |
| 3:20–4:15 | Apoyo PT-14..16 | Apoyo | Apoyo tokens | Ataques 3, 5, 6 + evidencias | Ataques 1, 2, 4, 7 |
| 4:15–5:00 | README | PR final | Revisa matrices | OWASP + informe 1-3 | Matriz de acceso + informe 4-6 |

Regla de integración: a las **2:30** nadie abre más frentes; primero `develop` compila y las 17
pruebas pasan, después se sigue con ataques y documentos.

---

## 6. Git, PR y revisiones

| Rama | Autor | Revisor |
| --- | --- | --- |
| `feature/setup` | Brian | Juan Esteban |
| `feature/persistence` | Juan Esteban | Brian |
| `feature/auth-registration` / `feature/auth-login` | Juan José | Santiago |
| `feature/jwt` | Brian | Juan Esteban |
| `feature/security-config` | Brian | Diego |
| `feature/requests-core` | Juan Esteban | Brian |
| `feature/web-accounts` / `feature/profiles-admin` | Santiago | Brian |
| `feature/requests-api` | Diego | Juan José |
| `feature/docs` | Santiago y Diego | Juan José / Brian |

Formato de commit: `feat:`, `fix:`, `test:`, `docs:`, `chore:`.
Al final: PR de `develop` a `main`.

Cada integrante revisa al menos un PR:

- Juan Esteban revisa a Brian (setup/JWT).
- Juan José revisa a Diego.
- Brian revisa a Juan Esteban y a Santiago.
- Santiago revisa a Juan José.
- Diego revisa a Brian.

---

## 7. Cómo evitar bloqueos

1. A las 0:50 los DTOs y los servicios (clases `@Service`) están en `develop`; desde ahí Santiago y Diego
   compilan contra el contrato aunque la lógica todavía no esté completa.
2. Santiago y Diego **no esperan** implementaciones: escriben controladores y pruebas primero; las
   corren cuando los servicios estén mergeados (2:30).
3. Si un servicio no está listo, se puede levantar la app con una implementación temporal en la rama
   del controlador, pero **no se mergea**.
4. Los cambios de contrato se anuncian en el chat y los aplica el dueño del archivo.
5. Si alguien termina antes: ayuda con PT-14..16 o con evidencias, sin editar archivos ajenos.
6. Juan Esteban es el único que puede desbloquear la base; si se atrasa, Brian lo apoya con las
   entidades (es el autor del setup).

---

## 8. Checklist final (Definition of Done)

- [ ] `mvn clean verify` en verde con PT-01 a PT-17.
- [ ] Contraseñas con BCrypt y sin texto plano en la base.
- [ ] JWT HS256 con `sub`, `role`, `iss`, `iat`, `exp` y 30 min.
- [ ] Sin token, alterado o vencido → `401` con `ApiError`.
- [ ] Rol incorrecto → `403`; solicitud ajena → `404`.
- [ ] Perfil público sin correo ni teléfono.
- [ ] Sin secretos en el repositorio ni en logs.
- [ ] `docs/ataques.md`, `docs/owasp.md`, `docs/matriz-acceso.md`, `docs/informe-seguridad.md` y
      `docs/evidence/**` completos.
- [ ] Los 5 integrantes con commits y revisiones.

---

## 9. Base de datos local (PostgreSQL)

| Tarea | Quién | Cuándo |
| --- | --- | --- |
| Levantar el contenedor `postgres-oficioya` y crear `oficioya_security` | Juan Esteban | Bloque 1 |
| Crear el `.env` local con `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_CORREO` y `ADMIN_PASSWORD` | Juan Esteban | Bloque 1 |
| Crear `.env.example` sin valores y verificar que `.env` no se suba | Brian | Bloque 1 |
| Configurar H2 para las pruebas (`application-test.properties`) | Juan Esteban | LAB-29 |
| Tomar la evidencia del hash en `usuarios.password_hash` | Santiago | LAB-54 |

Estado actual: **Docker Desktop está apagado y el puerto 5432 está cerrado**. No hay `.env` local.

### Pasos

```bash
# 1. Encender Docker Desktop y crear el contenedor
docker run --name postgres-oficioya \
  -e POSTGRES_DB=oficioya_security \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=<su_clave> \
  -p 5432:5432 -d postgres:17

# 2. Verificar
docker ps
docker exec -it postgres-oficioya psql -U postgres -d oficioya_security -c "\dt"
```

`.env` local (nunca se sube):

```properties
DB_PASSWORD=<su_clave>
JWT_SECRET=<openssl rand -base64 48>
ADMIN_CORREO=admin@oficioya.com
ADMIN_PASSWORD=<clave_del_admin>
```

En PowerShell, las variables se cargan así antes de arrancar:

```powershell
$env:DB_PASSWORD="<su_clave>"
$env:JWT_SECRET="<clave_de_48_bytes>"
$env:ADMIN_CORREO="admin@oficioya.com"
$env:ADMIN_PASSWORD="<clave_del_admin>"
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run
```

En Git Bash o Linux:

```bash
set -a && source .env && set +a
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

### Dos bases, no una

- **Desarrollo y ataques:** PostgreSQL en Docker (perfil `dev`, con el `ADMIN` sembrado).
- **Pruebas:** H2 en memoria (`@ActiveProfiles("test")`), sin Docker y sin tocar la base real.

La evidencia del hash se toma contra PostgreSQL:

```sql
SELECT correo, password_hash FROM usuarios;
-- password_hash debe empezar por $2a$ / $2b$ y no ser texto plano
```

---

## 10. Calendario y entrega (sin contar el 25-oct)

Se descarta el domingo 25. Quedan **lunes 26 a jueves 29** (4 días). Hay dos escenarios.

### Escenario A · entrega jueves 29 (comprimido, riesgo alto)

| Fecha | Día | Franja | Horas | Objetivo | Criterio de salida |
| --- | --- | --- | --- | --- | --- |
| 26-oct | Lun | 18:00–22:00 | 4 | Fase 0 + implementación completa | Base mergeada, endpoints, JWT y servicios compilando |
| 27-oct | Mar | 18:00–20:30 | 2.5 | Integración y pruebas | `mvn clean verify` con PT-01 a PT-17 en verde |
| 28-oct | Mié | 18:00–20:00 | 2 | Ataques, evidencias y docs | Ataques 1-7 registrados y docs al 90 % |
| 29-oct | Jue | 16:00–17:30 | 1.5 | Cierre y entrega | `main` actualizado, README y DoD completos |

Total: **10 h de calendario y cero holgura**. Si el lunes no queda compilando todo, el 29 no sale.

### Escenario B · entrega viernes 30 (recomendado)

| Fecha | Día | Franja | Horas | Objetivo | Criterio de salida |
| --- | --- | --- | --- | --- | --- |
| 26-oct | Lun | 18:00–20:30 | 2.5 | Fase 0: contrato, entidades, repos, seguridad base | Base mergeada, `mvn compile` verde y PostgreSQL arriba |
| 27-oct | Mar | 18:00–20:30 | 2.5 | Implementación de los 4 frentes | Endpoints, JWT, filtro y servicios compilando |
| 28-oct | Mié | 18:00–20:00 | 2 | Integración y pruebas | `mvn clean verify` con PT-01 a PT-17 en verde |
| 29-oct | Jue | 18:00–20:00 | 2 | Ataques, evidencias y documentación | Ataques 1-7 y docs al 90 % |
| 30-oct | Vie | 17:00–18:00 | 1 | Cierre y entrega | `main` actualizado, README y DoD completos |

Total: **10 h de calendario con 1 día de colchón**.

### Veredicto

**Sin el domingo, el 29 deja de ser cómodo.** Solo sale si el lunes se trabajan 4 horas y nada falla.
La recomendación es **entregar el viernes 30** y usar el jueves como colchón.

Si el 29 es obligatorio, se usa el escenario A: el lunes se hace base + implementación en una sola
sesión y el martes se prueba todo. Cualquier fallo del martes mata la entrega del 29.

### Puntos de control

| Cuándo | Qué se revisa | Si falla |
| --- | --- | --- |
| Lun 26, 22:00 (A) / 20:30 (B) | ¿La base está mergeada y compila? | Se recorta LAB-32 (Postman) y se sigue |
| Mar 27, 20:30 (A) | **`mvn clean verify` en verde** | No se entrega el 29; se pasa al escenario B |
| Mié 28, 20:00 (B) | **`mvn clean verify` en verde** | Entrega el 30; se avisa ese día |
| Jue 29, 20:00 | ¿Docs y ataques completos? | El viernes en la mañana se termina |
| Vie 30, 12:00 | Verificación final y PR a `main` | Se entrega solo sin fallos de seguridad abiertos |

### Qué se recorta primero si no se llega

1. Colección Postman y `.http` (LAB-32).
2. Evidencias adicionales de jwt.io (LAB-54), dejando solo 401/403/404 y hash.
3. Preguntas 4-6 del informe a un párrafo cada una.
4. **Nunca se recorta:** JWT, filtro, autorización por rol/propiedad, BCrypt ni las 17 pruebas.

---

## Anexo · Lista tarea-persona

> Esta lista **solo contiene trabajo pendiente**. El andamiaje (proyecto, dependencias y paquetes)
> ya está en `feature/setup`; no se repite.

| ID | Tarea | Persona | Depende de | Rama |
| --- | --- | --- | --- | --- |
| LAB-18 | Mergear `feature/setup` a `develop` y cerrar Bloque 0 (`.gitignore`, `.env.example`, `pom`) | Brian | — | `feature/setup` |
| LAB-19 | Contenedor PostgreSQL + `.env` local | Juan Esteban | — | `feature/persistence` |
| LAB-20 | Entidades `Usuario`, `Solicitud` y enums | Juan Esteban | — | `feature/persistence` |
| LAB-21 | Repositorios con consultas por correo y por participante | Juan Esteban | LAB-20 | `feature/persistence` |
| LAB-22 | `ApiError` y excepciones propias | Juan Esteban | — | `feature/persistence` |
| LAB-23 | DTOs de auth y usuarios | Juan José | — | `feature/auth-registration` |
| LAB-24 | DTOs de solicitudes | Brian | — | `feature/jwt` |
| LAB-25 | Servicios `AuthService` y `UsuarioService` (clases `@Service`) | Juan José | LAB-23 | `feature/auth-registration` |
| LAB-26 | Servicio `SolicitudService` (clase `@Service`) | Brian | LAB-24 | `feature/jwt` |
| LAB-27 | `SecurityConfig` base + `PasswordEncoder` | Brian | — | `feature/security-config` |
| LAB-28 | `JwtService` (generar y validar) | Brian | — | `feature/jwt` |
| LAB-29 | `application-test.properties` con H2 | Juan Esteban | — | `feature/persistence` |
| LAB-30 | Esqueletos de controladores de cuentas/perfiles | Santiago | LAB-23, LAB-25 | `feature/web-accounts` |
| LAB-31 | Esqueleto de `SolicitudController` | Diego | LAB-24, LAB-26 | `feature/requests-api` |
| LAB-32 | Carpetas de docs, `.http` y colección Postman | Santiago / Diego | — | `feature/docs` |
| LAB-33 | `AuthService.register` (BCrypt, 409, roles públicos) | Juan José | LAB-20, LAB-21, LAB-23 | `feature/auth-registration` |
| LAB-34 | `AuthService.login` (matches + mensaje genérico) | Juan José | LAB-28, LAB-33 | `feature/auth-login` |
| LAB-35 | `UsuarioService` (`/me`, trabajadores, admin) | Juan José | LAB-20, LAB-21, LAB-23 | `feature/auth-login` |
| LAB-36 | Mappers de usuarios | Juan José | LAB-23 | `feature/auth-login` |
| LAB-37 | `@RestControllerAdvice` completo (400/401/403/404/409) | Juan José | LAB-22 | `feature/auth-registration` |
| LAB-38 | `JwtAuthenticationFilter` | Brian | LAB-28, LAB-27 | `feature/security-config` |
| LAB-39 | EntryPoint `401` y AccessDeniedHandler `403` | Brian | LAB-27 | `feature/security-config` |
| LAB-40 | `SecurityConfig` final: stateless, reglas, filtro | Brian | LAB-27, LAB-38, LAB-39 | `feature/security-config` |
| LAB-41 | `SolicitudService`: crear, listar y obtener con propiedad | Juan Esteban | LAB-20, LAB-21, LAB-26 | `feature/requests-core` |
| LAB-42 | `SolicitudService`: aceptar, rechazar, cancelar y `409` | Juan Esteban | LAB-41 | `feature/requests-core` |
| LAB-43 | `AuthController` (registro `201`, login `200`) | Santiago | LAB-25, LAB-30 | `feature/web-accounts` |
| LAB-44 | `UsuarioController`, `TrabajadorController`, `AdminController` | Santiago | LAB-25, LAB-30 | `feature/profiles-admin` |
| LAB-45 | `AdminSeeder` solo perfil `dev` | Santiago | LAB-20, LAB-21, LAB-27 | `feature/profiles-admin` |
| LAB-46 | `SolicitudController` con los 6 endpoints y `@PreAuthorize` | Diego | LAB-26, LAB-31 | `feature/requests-api` |
| LAB-47 | PT-01 a PT-06 | Juan José | LAB-33, LAB-34 | `feature/auth-login` |
| LAB-48 | PT-07 a PT-09 | Brian | LAB-28, LAB-40 | `feature/security-config` |
| LAB-49 | PT-14 a PT-16 | Diego | LAB-41, LAB-42 | `feature/requests-api` |
| LAB-50 | PT-10 a PT-12 | Santiago | LAB-35, LAB-44, LAB-45 | `feature/profiles-admin` |
| LAB-51 | PT-13 y PT-17 | Diego | LAB-46 | `feature/requests-api` |
| LAB-52 | Ataques 1, 2, 4 y 7 | Diego | app con perfil `dev` | `feature/docs` |
| LAB-53 | Ataques 3, 5 y 6 | Santiago | app con perfil `dev` | `feature/docs` |
| LAB-54 | Evidencias `401/403/404`, hash en PostgreSQL y jwt.io | Santiago | ataques | `feature/docs` |
| LAB-55 | Matriz OWASP + preguntas 1 a 3 del informe | Santiago | LAB-47, LAB-48 | `feature/docs` |
| LAB-56 | Matriz de acceso + preguntas 4 a 6 del informe | Diego | LAB-49, LAB-51 | `feature/docs` |
| LAB-57 | README: ejecución, variables, endpoints y `mvn clean verify` | Juan Esteban | todo | `feature/docs` |
| LAB-58 | `mvn clean verify` en verde | Todos | LAB-47 a LAB-51 | `develop` |
| LAB-59 | PR final `develop` → `main` | Juan José | LAB-58 | `develop` |

### Resumen por persona

- **Juan Esteban (8):** LAB-19, LAB-20, LAB-21, LAB-22, LAB-29, LAB-41, LAB-42, LAB-57.
- **Juan José (9):** LAB-23, LAB-25, LAB-33, LAB-34, LAB-35, LAB-36, LAB-37, LAB-47, LAB-59.
- **Brian (9):** LAB-18, LAB-24, LAB-26, LAB-27, LAB-28, LAB-38, LAB-39, LAB-40, LAB-48.
- **Santiago (8):** LAB-30, LAB-43, LAB-44, LAB-45, LAB-50, LAB-53, LAB-54, LAB-55.
- **Diego (7):** LAB-31, LAB-32, LAB-46, LAB-49, LAB-51, LAB-52, LAB-56.

### Camino crítico

`LAB-20 → LAB-21 → LAB-28 → LAB-34` (login) y `LAB-20 → LAB-21 → LAB-26 → LAB-41 → LAB-46` (solicitudes).

---

## Anexo · Tarea, responsable y fecha

Fechas del **escenario A (entrega 29)**. Si eligen el escenario B (30), cada bloque corre un día
después y el cierre pasa al viernes 30.

| Tarea | Quién | Fecha |
| --- | --- | --- |
| LAB-18 · Mergear `feature/setup` y cerrar Bloque 0 | Brian | Lun 26-oct |
| LAB-19 · Contenedor PostgreSQL + `.env` local | Juan Esteban | Lun 26-oct |
| LAB-20 · Entidades y enums | Juan Esteban | Lun 26-oct |
| LAB-21 · Repositorios | Juan Esteban | Lun 26-oct |
| LAB-22 · `ApiError` y excepciones | Juan Esteban | Lun 26-oct |
| LAB-23 · DTOs de auth y usuarios | Juan José | Lun 26-oct |
| LAB-24 · DTOs de solicitudes | Brian | Lun 26-oct |
| LAB-25 · Servicios de auth y usuarios | Juan José | Lun 26-oct |
| LAB-26 · Servicio `SolicitudService` | Brian | Lun 26-oct |
| LAB-27 · `SecurityConfig` base + `PasswordEncoder` | Brian | Lun 26-oct |
| LAB-28 · `JwtService` | Brian | Lun 26-oct |
| LAB-29 · Perfil `test` con H2 | Juan Esteban | Lun 26-oct |
| LAB-30 · Esqueletos de controladores de cuentas/perfiles | Santiago | Lun 26-oct |
| LAB-31 · Esqueleto de `SolicitudController` | Diego | Lun 26-oct |
| LAB-32 · Docs, `.http` y Postman | Santiago y Diego | Lun 26-oct |
| LAB-33 · Registro con BCrypt | Juan José | Lun 26-oct |
| LAB-34 · Login con mensaje genérico | Juan José | Lun 26-oct |
| LAB-35 · `UsuarioService` | Juan José | Lun 26-oct |
| LAB-36 · Mappers de usuarios | Juan José | Lun 26-oct |
| LAB-37 · Advice completo | Juan José | Lun 26-oct |
| LAB-38 · `JwtAuthenticationFilter` | Brian | Lun 26-oct |
| LAB-39 · Handlers `401` y `403` | Brian | Lun 26-oct |
| LAB-40 · `SecurityConfig` final | Brian | Lun 26-oct |
| LAB-41 · Solicitudes: crear, listar, obtener | Juan Esteban | Lun 26-oct |
| LAB-42 · Solicitudes: aceptar, rechazar, cancelar | Juan Esteban | Lun 26-oct |
| LAB-43 · `AuthController` | Santiago | Lun 26-oct |
| LAB-44 · Controladores de perfiles y admin | Santiago | Lun 26-oct |
| LAB-45 · `AdminSeeder` | Santiago | Lun 26-oct |
| LAB-46 · `SolicitudController` | Diego | Lun 26-oct |
| LAB-47 · PT-01 a PT-06 | Juan José | Mar 27-oct |
| LAB-48 · PT-07 a PT-09 | Brian | Mar 27-oct |
| LAB-49 · PT-14 a PT-16 | Diego | Mar 27-oct |
| LAB-50 · PT-10 a PT-12 | Santiago | Mar 27-oct |
| LAB-51 · PT-13 y PT-17 | Diego | Mar 27-oct |
| LAB-58 · `mvn clean verify` de integración | Todos | Mar 27-oct |
| LAB-52 · Ataques 1, 2, 4 y 7 | Diego | Mié 28-oct |
| LAB-53 · Ataques 3, 5 y 6 | Santiago | Mié 28-oct |
| LAB-54 · Evidencias `401/403/404`, hash y jwt.io | Santiago | Mié 28-oct |
| LAB-55 · Matriz OWASP + preguntas 1 a 3 | Santiago | Mié 28-oct |
| LAB-56 · Matriz de acceso + preguntas 4 a 6 | Diego | Mié 28-oct |
| LAB-57 · README | Juan Esteban | Jue 29-oct |
| LAB-58 · Repetir `mvn clean verify` antes de entregar | Todos | Jue 29-oct |
| LAB-59 · PR final `develop` → `main` | Juan José | Jue 29-oct |

> Las tareas del 26 corren en paralelo por frentes, no en fila india. Las revisiones de PR
> se hacen el mismo día en que se abre el PR (matriz en la sección 6). Si el 27 no hay `verify`
> verde, se aplica el plan B: entrega el viernes 30.

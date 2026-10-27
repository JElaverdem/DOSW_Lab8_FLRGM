# Descripción de tareas · Lab 8 Seguridad (OficioYa)

Cada tarea remite al README del laboratorio: secciones `§`, requerimientos `RS-xx`, bloques `Bloque N`
y pruebas `PT-xx`. Las fechas y el orden están en `docs/plan-equipo.md`.

---

## Fase 0 · Base común y contratos

| ID / Tarea | Quién | Descripción | Cita README |
| --- | --- | --- | --- |
| LAB-18 · Cerrar Bloque 0 | Brian | Mergear la rama `feature/setup` que ya existe, corregir `.gitignore` (la regla `.env*` bloquea `.env.example`; agregar `!.env.example`), crear `.env.example` con `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_CORREO` y `ADMIN_PASSWORD` sin valores, y ordenar el `pom.xml` (jjwt juntos, starters de prueba redundantes). Criterio: `develop` con el esqueleto, `mvn -DskipTests compile` verde y ningún secreto versionado. | §9 Bloque 0 pasos 3-5; §5.1 reglas 4 y 6; §14 |
| LAB-19 · PostgreSQL y `.env` local | Juan Esteban | Levantar `postgres:17` con la base `oficioya_security`, exponer el puerto 5432 y crear el `.env` local (ignorado por Git) con `DB_PASSWORD`, `JWT_SECRET` de al menos 32 bytes, `ADMIN_CORREO` y `ADMIN_PASSWORD`. Verificar con `docker ps` y `\dt`. | §8 base de datos con Docker; §5.1 regla 4; RS-10 |
| LAB-20 · Entidades y enums | Juan Esteban | Crear `Usuario` (id, nombre, correo único, teléfono, `password_hash`, rol, oficio principal, zona, fecha de creación) y `Solicitud` (id, contratante, trabajador, descripción, estado, fecha), con ids `Long` autogenerados, enums guardados como texto y relaciones `@ManyToOne` obligatorias. | §7 modelo de datos; §8 paquete `entity`; Bloque 1.1 |
| LAB-21 · Repositorios | Juan Esteban | Crear `UsuarioRepository` (`findByCorreo`, `existsByCorreo`) y `SolicitudRepository` con consultas que incluyan al participante (`findByIdAndContratanteId`, `findByIdAndTrabajadorId`, `findByContratanteIdOrTrabajadorId`). La consulta debe impedir traer una solicitud ajena. | Bloque 1.2; Bloque 6.3; RS-07 |
| LAB-22 · `ApiError` y excepciones | Juan Esteban | Crear el record `ApiError` (timestamp, status, error, mensaje, ruta) con factory `of`, y las excepciones propias: correo duplicado, rol no permitido, credenciales inválidas, recurso no encontrado y estado inválido. No llamarlo `ErrorResponse` para no chocar con Spring. | §6 formato único de error; Bloque 1.4; Bloque 2.4; RS-09 |
| LAB-23 · DTOs de auth y usuarios | Juan José | Definir `RegistroRequest` con `@NotBlank`, `@Email`, `@Size(min = 8)` y `@NotNull`; `RegistroResponse` sin contraseña ni hash; `LoginRequest`/`LoginResponse`; `UsuarioMeResponse` con contacto; `PerfilPublicoResponse` sin correo ni teléfono; `PerfilAdminResponse` con contacto. | §6 ejemplos; RS-01, RS-03 y RS-06; Bloque 2.1; Bloque 5 |
| LAB-24 · DTOs de solicitudes | Brian | Definir `SolicitudRequest` (`trabajadorId`, `descripcion`) y `SolicitudResponse` (id, participantes, descripción, estado, fecha). El contratante **no** se recibe en el request: sale del token. | §6 crear solicitud; RS-08; Bloque 6.2 |
| LAB-25 · Interfaces de auth y usuarios | Juan José | Definir `AuthService` (`registrar`, `login`) y `UsuarioService` (`me`, `listarTrabajadores`, `obtenerTrabajador`, `listarParaAdmin`). Son el contrato congelado que permite que los controladores se escriban en paralelo. | §8 paquete `service`; §3.1 paralelismo; Bloques 2 y 5 |
| LAB-26 · Interfaz de solicitudes | Brian | Definir `SolicitudService` con `crear`, `listar`, `obtener`, `aceptar`, `rechazar` y `cancelar`, todos recibiendo el correo autenticado. La identidad nunca viaja en el cuerpo ni en la URL. | §6 contrato; RS-07 y RS-08; §5.1 regla 2; Bloque 6 |
| LAB-27 · `SecurityConfig` base | Brian | Crear la configuración con `@EnableMethodSecurity`, bean `PasswordEncoder` BCrypt, CSRF desactivado, sesiones stateless, `/api/v1/auth/**` público, `/api/v1/admin/**` solo `ADMIN` y `anyRequest().authenticated()`. Prohibido `anyRequest().permitAll()` y `NoOpPasswordEncoder`. | RS-05; Bloque 1.4; Bloque 4.1; prohibiciones de §5.1 |
| LAB-28 · `JwtService` | Brian | Leer clave, emisor y duración desde `app.jwt.*`; generar HS256 con `sub` (correo), `role`, `iss`, `iat` y `exp` de 30 minutos; validar firma, expiración y emisor. La duración debe ser configurable para poder generar un token vencido en las pruebas. | §5.1 reglas 3, 4 y 7; RS-04; Bloque 3.1-3.2 |
| LAB-29 · Perfil `test` con H2 | Juan Esteban | Crear `application-test.properties` con H2 en memoria, `ddl-auto=create-drop`, clave/issuer/ttl solo de pruebas. Objetivo: que `mvn test` no dependa de PostgreSQL. | Bloque 1.3; Bloque 7; ejemplo de §9 |
| LAB-30 · Esqueletos de controladores de cuentas | Santiago | Crear `AuthController`, `UsuarioController`, `TrabajadorController` y `AdminController` con sus rutas, verbos y firmas contra las interfaces, sin lógica todavía. Deben compilar antes de que existan los servicios. | §6 contrato REST; §8 paquete `controller`; §3 paralelismo |
| LAB-31 · Esqueleto de `SolicitudController` | Diego | Crear la clase con los seis endpoints de solicitudes y las firmas contra `SolicitudService`; dejar marcado dónde va cada `@PreAuthorize` por rol. | §6 contrato; §8 paquete `controller`; RS-08 |
| LAB-32 · Docs, `.http` y Postman | Santiago y Diego | Crear `docs/evidence/`, `docs/ataques.md`, `docs/http/oficioya.http` y la colección Postman con los endpoints del contrato. No incluir tokens reales ni secretos. | §10 evidencias; Bloque 8; Bloque 9 |

---

## Implementación

| ID / Tarea | Quién | Descripción | Cita README |
| --- | --- | --- | --- |
| LAB-33 · Registro con BCrypt | Juan José | Implementar `AuthServiceImpl.registrar`: normalizar correo (trim + minúsculas), responder 409 si existe, aceptar solo `TRABAJADOR` y `CONTRATANTE` (400 si no), hashear con BCrypt y devolver 201 sin hash. `oficioPrincipal` y `zona` solo aplican a trabajadores. | RS-01 y RS-02; §5.1 regla 1; Bloque 2; PT-01 a PT-04 |
| LAB-34 · Login | Juan José | Implementar `POST /auth/login`: buscar por correo normalizado, comparar con `PasswordEncoder.matches`, responder 401 con el **mismo** mensaje si el correo no existe o la clave es incorrecta, y devolver `Bearer` + token + `expiraEn` (1800). | RS-03; §5.1 regla 5; §6 ejemplo de login; Bloque 3.3; PT-05 y PT-06 |
| LAB-35 · `UsuarioServiceImpl` | Juan José | Implementar `/usuarios/me` con el contacto del usuario autenticado; listar trabajadores con DTO público; obtener un trabajador por id o 404; listar para el `ADMIN` con contacto. | RS-06; §6; Bloque 5.1-5.3; PT-10 y PT-12 |
| LAB-36 · Mappers de usuarios | Juan José | Convertir entidad ↔ DTO sin devolver nunca la entidad JPA en la respuesta. El mapper de usuarios cubre registro, `/me`, perfil público y perfil admin. | §8 paquete `mapper`; prohibición de devolver entidades (§5.1); pista del Bloque 2 |
| LAB-37 · `@RestControllerAdvice` | Juan José | Completar el manejador global: 409 por correo duplicado, 400 por rol no permitido o datos inválidos, 400 por cuerpo ilegible, 404 por recurso no encontrado, 409 por estado inválido y 401 por credenciales. No capturar `AccessDeniedException` de forma genérica, o el 403 se convertirá en 500. | RS-09; §6 formato único; Bloque 2.4; advertencia del Bloque 5 |
| LAB-38 · `JwtAuthenticationFilter` | Brian | Implementar `OncePerRequestFilter`: leer `Authorization: Bearer`, validar el token y dejar en el `SecurityContext` un `UsernamePasswordAuthenticationToken` con autoridad `ROLE_<rol>`. Si el token falla, limpiar el contexto y continuar; el entry point responderá 401. Registrar con `addFilterBefore`. | RS-04 y RS-05; Bloque 4.2; §5.1 regla 3 |
| LAB-39 · Handlers 401 y 403 | Brian | Implementar `AuthenticationEntryPoint` y `AccessDeniedHandler` que escriban `ApiError` en JSON (UTF-8) con 401 y 403. Sin esto, Spring puede responder 403 a una petición sin token. | RS-09; §6 formato único; Bloque 4.3; PT-07 |
| LAB-40 · `SecurityConfig` final | Brian | Integrar el filtro y los handlers a la cadena: stateless, CSRF off, `/auth/**` público, `/admin/**` solo `ADMIN`, resto autenticado y seguridad por método activa. Verificar que sin token se responde 401 y no 403. | RS-05; Bloque 4.1; pista del Bloque 4; PT-07 a PT-09 |
| LAB-41 · Solicitudes: crear, listar y obtener | Juan Esteban | Implementar la creación con el contratante sacado del contexto, estado inicial `PENDIENTE` y fecha de creación; listar solo las solicitudes donde el usuario es contratante o trabajador; obtener solo si participa, con 404 para ajenas o inexistentes. | RS-07 y RS-08; §6; Bloque 6.2-6.3; PT-14 y PT-15 |
| LAB-42 · Solicitudes: transiciones | Juan Esteban | Implementar aceptar/rechazar solo para el trabajador asignado y solo desde `PENDIENTE`; cancelar solo para el contratante autor y solo desde `PENDIENTE` o `ACEPTADA`; cualquier otra transición responde 409. Una solicitud ajena responde 404. | §4.2 estados; RS-07 y RS-08; §6; Bloque 6; PT-15 y PT-16 |
| LAB-43 · `AuthController` | Santiago | Exponer `POST /api/v1/auth/registro` (201) y `POST /api/v1/auth/login` (200) con `@Valid`, delegando en `AuthService`. No debe devolver entidades ni el hash. | §6 contrato; RS-01 y RS-03; Bloques 2.3 y 3.3 |
| LAB-44 · Controladores de perfiles y admin | Santiago | Exponer `/usuarios/me`, `/trabajadores`, `/trabajadores/{id}` y `/admin/usuarios`. El perfil público no lleva correo ni teléfono; el de admin sí. Proteger `/admin/**` con `@PreAuthorize("hasRole('ADMIN')")`. | §6 contrato; RS-06; Bloque 5; PT-10 a PT-12 |
| LAB-45 · `AdminSeeder` | Santiago | Crear el `ADMIN` al arrancar solo con `@Profile("dev")`, leyendo `ADMIN_CORREO` y `ADMIN_PASSWORD`, con BCrypt y de forma idempotente. Nunca debe poder crearse un `ADMIN` por el registro público. | §5.1 regla 6; Bloque 5.4; §4.1 roles; RS-01 |
| LAB-46 · `SolicitudController` | Diego | Exponer los seis endpoints de solicitudes con `@PreAuthorize`: solo `CONTRATANTE` crea y cancela; solo el `TRABAJADOR` asignado acepta o rechaza; `ADMIN` no accede. La identidad se toma de `Authentication.getName()`. | §6 contrato; RS-08; Bloque 6; PT-13 y PT-17 |

---

## Pruebas

| ID / Tarea | Quién | Descripción | Cita README |
| --- | --- | --- | --- |
| LAB-47 · PT-01 a PT-06 | Juan José | Cubrir registro válido (201, sin hash), correo repetido (409), rol `ADMIN` o clave corta (400), hash BCrypt distinto del texto, login correcto (200 con token) y login fallido (401 con el mismo mensaje para correo inexistente y clave errónea). | Bloque 7, tabla PT-01 a PT-06; RS-01 a RS-03 |
| LAB-48 · PT-07 a PT-09 | Brian | Cubrir recurso protegido sin token (401 con `ApiError`), token alterado en un carácter (401) y token vencido (401). Generar los tokens con `JwtService`, usando TTL negativo para el vencido. | Bloque 7, tabla PT-07 a PT-09 y ejemplo de token alterado/vencido; RS-04 |
| LAB-49 · PT-14 a PT-16 | Diego | Cubrir que el contratante solo lista las propias, que consultar o cancelar una solicitud ajena da 404 sin cambios en la base, que aceptar con un trabajador no asignado da 404 y con un contratante da 403. | Bloque 7, tabla PT-14 a PT-16; RS-07 y RS-08; Bloque 6 |
| LAB-50 · PT-10 a PT-12 | Santiago | Cubrir perfil público sin `correo` ni `telefono`, `CONTRATANTE` en `/admin/usuarios` (403) y `ADMIN` en `/admin/usuarios` (200 con contacto). Usar `@WithMockUser`. | Bloque 7, tabla PT-10 a PT-12 y ejemplo con `@WithMockUser`; RS-06 |
| LAB-51 · PT-13 y PT-17 | Diego | Cubrir que un `TRABAJADOR` no puede crear solicitud (403) y que un `ADMIN` no puede consultar `/solicitudes` (403). | Bloque 7, tabla PT-13 y PT-17; RS-08 |

---

## Ataques, evidencias, documentación y cierre

| ID / Tarea | Quién | Descripción | Cita README |
| --- | --- | --- | --- |
| LAB-52 · Ataques 1, 2, 4 y 7 | Diego | Documentar con `curl`/Postman: `/solicitudes` sin token (401), token manipulado en jwt.io cambiando `role` a `ADMIN` (401), token de un contratante sobre una solicitud de otro (404) y login con correo inexistente vs. clave errónea (mismo 401 y mismo mensaje). | Bloque 8, ataques 1, 2, 4 y 7; §10 |
| LAB-53 · Ataques 3, 5 y 6 | Santiago | Documentar: token vencido (401), contratante llamando `/admin/usuarios` (403) y registro con `"rol": "ADMIN"` (400). | Bloque 8, ataques 3, 5 y 6 |
| LAB-54 · Evidencias | Santiago | Guardar en `docs/evidence/`: columna `password_hash` en PostgreSQL con un hash BCrypt, payload decodificado del token en jwt.io sin el token completo, respuestas 401/403/404, ataques 2 y 4, y salida de `mvn clean verify`. | §10 evidencias; Bloque 8 |
| LAB-55 · Matriz OWASP + informe 1-3 | Santiago | Completar la matriz con A01, A02, A04, A05, A07 y A10, indicando dónde ocurriría en OficioYa, el control implementado y la prueba o evidencia. Responder las preguntas 1 a 3 del informe con ejemplos propios. | §11 matriz OWASP; §13 preguntas 1-3 |
| LAB-56 · Matriz de acceso + informe 4-6 | Diego | Elegir al menos 6 endpoints del dominio y completar roles, regla de propiedad/datos ocultos y código de error. Responder las preguntas 4 a 6 del informe (identidad desde el token, CSRF y revocación, secretos y producción). | §12 matriz de acceso; §13 preguntas 4-6 |
| LAB-57 · README del repositorio | Juan Esteban | Actualizar el README con cómo ejecutar, variables de entorno sin valores, contrato de endpoints, resultado de `mvn clean verify` y enlaces a evidencias, matrices e informe. | Bloque 9; §15 Definition of Done; §14 |
| LAB-58 · `mvn clean verify` | Todos | Ejecutar la suite completa con el perfil `test` y H2; las 17 pruebas deben pasar. Es la barrera para mergear el PR final. | Bloque 7; §15 Definition of Done |
| LAB-59 · PR final `develop` → `main` | Juan José | Abrir el PR final, verificar que cada integrante tenga commits y al menos una revisión, y mergear a `main` solo con `mvn clean verify` verde. | §14 buenas prácticas de Git; §15 |

---

## Referencia rápida de citas

- `§5` y `§5.1`: requerimientos de seguridad y reglas obligatorias.
- `§6`: contrato REST y formato único de error.
- `§7`: modelo de datos.
- `§9 Bloques 0-9`: desarrollo paso a paso.
- `PT-01` a `PT-17`: matriz de pruebas del Bloque 7.
- `§10`: evidencias; `§11`: OWASP; `§12`: matriz de acceso; `§13`: informe.
- `§14`: Git y PR; `§15`: evaluación y Definition of Done.

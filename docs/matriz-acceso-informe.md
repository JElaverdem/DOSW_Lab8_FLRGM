# Matriz de acceso · OficioYa (LAB-56)

## 1. Matriz de acceso

Roles: `CONTRATANTE`, `TRABAJADOR`, `ADMIN`. "Autenticado" significa cualquier usuario con token válido.
Todas las rutas protegidas responden **401** si no hay token o el token es inválido o está vencido.

| # | Endpoint | Roles permitidos | Regla de propiedad / datos ocultos | Código de error |
| --- | --- | --- | --- | --- |
| 1 | `POST /api/v1/auth/registro` | Público | Solo se aceptan los roles `TRABAJADOR` y `CONTRATANTE`; nunca `ADMIN`. La respuesta no incluye contraseña ni hash. | 400 (rol no permitido o datos inválidos), 409 (correo duplicado) |
| 2 | `POST /api/v1/auth/login` | Público | Mismo mensaje si el correo no existe o la clave es incorrecta (evita enumerar usuarios). | 401 |
| 3 | `GET /api/v1/usuarios/me` | Autenticado | Solo devuelve los datos del usuario del token, incluido su contacto. | 401 |
| 4 | `GET /api/v1/trabajadores` | Autenticado | Listado público: no incluye correo ni teléfono. | 401 |
| 5 | `GET /api/v1/trabajadores/{id}` | Autenticado | Perfil público: no incluye correo ni teléfono. | 401, 404 (id inexistente o rol distinto de TRABAJADOR) |
| 6 | `GET /api/v1/admin/usuarios` | `ADMIN` | Es el único endpoint que devuelve el contacto de otros usuarios. | 401, 403 (rol distinto de ADMIN) |
| 7 | `POST /api/v1/solicitudes` | `CONTRATANTE` | El contratante sale del token, no del cuerpo. Estado inicial `PENDIENTE`. | 400 (datos inválidos), 401, 403 (TRABAJADOR o ADMIN) |
| 8 | `GET /api/v1/solicitudes` | `CONTRATANTE`, `TRABAJADOR` | Solo lista las solicitudes donde el usuario participa. | 401, 403 (ADMIN) |
| 9 | `GET /api/v1/solicitudes/{id}` | `CONTRATANTE`, `TRABAJADOR` | Solo si el usuario participa; una solicitud ajena responde igual que una inexistente. | 401, 403 (ADMIN), 404 (ajena o inexistente) |
| 10 | `PATCH /api/v1/solicitudes/{id}/aceptar` | `TRABAJADOR` | Solo el trabajador asignado y solo desde `PENDIENTE`. | 401, 403 (CONTRATANTE o ADMIN), 404 (no asignado o inexistente), 409 (estado inválido) |
| 11 | `PATCH /api/v1/solicitudes/{id}/rechazar` | `TRABAJADOR` | Solo el trabajador asignado y solo desde `PENDIENTE`. | 401, 403 (CONTRATANTE o ADMIN), 404 (no asignado o inexistente), 409 (estado inválido) |
| 12 | `PATCH /api/v1/solicitudes/{id}/cancelar` | `CONTRATANTE` | Solo el contratante autor, desde `PENDIENTE` o `ACEPTADA`. | 401, 403 (TRABAJADOR o ADMIN), 404 (ajena o inexistente), 409 (estado inválido) |

Notas de lectura:
- El **403** lo produce `@PreAuthorize` cuando el rol no corresponde. El **404** lo produce el servicio cuando
  la consulta, que incluye al participante, no encuentra la solicitud. Se responde 404 y no 403 para no
  revelar que la solicitud existe.
- Las rutas ya usan el prefijo `/api/v1`, igual que `SecurityConfig` y los controladores.

---

## 2. Informe

### Pregunta 4 · ¿Por qué la identidad se toma del token y no del cuerpo ni de la URL?

Lo que llega en el cuerpo o en la URL lo controla quien hace la petición. Si `POST /solicitudes` recibiera un
`contratanteId`, un contratante malintencionado podría enviar el id de otro y crear solicitudes a su nombre.
Lo mismo ocurriría con una ruta como `/usuarios/5/solicitudes`: bastaría cambiar el número para ver datos ajenos.
Esa es la referencia directa insegura a objetos (IDOR) del control de acceso roto (OWASP A01).

El token, en cambio, está firmado con HS256 por el servidor. Solo si la firma, la expiración y el emisor son
válidos, el filtro deja la identidad en el `SecurityContext`, y los controladores la leen con
`Authentication.getName()`. Por eso `SolicitudRequest` solo trae `trabajadorId` y `descripcion`: el contratante
no es un dato de entrada. El rol sigue la misma lógica: el claim `role` se usa solo después de validar la firma,
y el ataque 2 (cambiar `role` a `ADMIN` en jwt.io) falla con 401 porque la firma deja de coincidir.

Además, el servicio consulta siempre por el id del recurso **y** el del participante autenticado
(`findByIdAndContratanteId`, `findByIdAndTrabajadorId`), de modo que conocer un id no da acceso.

### Pregunta 5 · ¿Por qué se desactiva CSRF y qué pasa con la revocación de tokens?

**CSRF.** El ataque CSRF aprovecha que el navegador adjunta cookies automáticamente a cada petición. OficioYa
es una API sin estado (`SessionCreationPolicy.STATELESS`) que autentica con el encabezado
`Authorization: Bearer`, y el navegador nunca añade ese encabezado por su cuenta: el cliente debe ponerlo.
Sin cookie de sesión no hay nada que un sitio malicioso pueda reutilizar, por eso desactivar CSRF es válido aquí.
Si algún día el token se guardara en una cookie, habría que volver a activar la protección CSRF y usar
`SameSite` y `HttpOnly`.

**Revocación.** Un JWT es válido hasta que vence, y el servidor no guarda estado, así que no se puede
"cerrar" un token concreto. Si un usuario es eliminado o cambia de rol, el token ya emitido sigue
funcionando hasta su `exp`. Las mitigaciones del proyecto y las posibles mejoras son:
- Duración corta: 30 minutos (`app.jwt.ttl-minutes`), lo que limita el daño de un token robado.
- Mejora posible: una lista de tokens revocados por `jti`, o verificar en base de datos que el usuario
  siga activo en cada petición, a costa de perder parte de la naturaleza sin estado.
- Mejora posible: tokens de acceso cortos con *refresh tokens* que sí se pueden revocar.
- Cambiar `JWT_SECRET` invalida todos los tokens a la vez, útil ante una filtración.

### Pregunta 6 · ¿Cómo se manejan los secretos y qué cambiaría en producción?

**Manejo actual.** Ningún secreto está en el código ni en el repositorio:
- `JWT_SECRET`, `DB_PASSWORD`, `ADMIN_CORREO` y `ADMIN_PASSWORD` se leen de variables de entorno
  (`application.properties` usa `${JWT_SECRET}` y `${DB_PASSWORD}` sin valor por defecto).
- El archivo `.env` real está en `.gitignore`; se versiona solo `.env.example`, sin valores.
- La clave de firma debe tener al menos 32 bytes; la del perfil `test` es distinta y solo sirve para pruebas.
- Las contraseñas se guardan con BCrypt (`password_hash`), nunca en texto plano, y no se devuelven en ninguna respuesta.
- El `ADMIN` se crea solo con `@Profile("dev")` y desde variables de entorno; no se puede crear por el registro público.

**Cambios para producción:**
- Guardar los secretos en un gestor (por ejemplo, el de la nube o *secrets* del orquestador), con rotación periódica.
- Servir todo por HTTPS; sin TLS el token viaja en claro.
- Cambiar `spring.jpa.hibernate.ddl-auto=update` por `validate` y manejar el esquema con migraciones.
- Usar en la base de datos un usuario con los privilegios mínimos, no el administrador.
- No registrar tokens, contraseñas ni cuerpos de petición en los logs, y mantener mensajes de error genéricos.
- Considerar límites de intentos de login (rate limiting) y un procedimiento para crear el primer `ADMIN` fuera del perfil `dev`.

---

## 3. Arquitectura JWT en el proyecto

En OficioYa, el JWT lo **emite** `JwtService` (bloque 3, paquete `security`) al hacer login, y lo **validan**
`JwtAuthenticationFilter` y `JwtService.validar` en cada petición protegida (bloque 4). La clave de firma no está
en el repositorio: llega por la variable de entorno `JWT_SECRET`, que `application.properties` lee sin valor por defecto
(`${JWT_SECRET}`). `.env.example` documenta la variable sin valor y `.env` está en `.gitignore`. Así la clave se comparte
entre entornos sin versionarse.

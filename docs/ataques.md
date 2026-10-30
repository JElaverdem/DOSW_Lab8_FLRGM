# Ataques 1, 2, 4 y 7 

Pruebas manuales contra la API en ejecución. Cada ataque indica la petición, el resultado esperado
y un espacio para el resultado real y la evidencia.

## Preparación

Variable base: BASE=http://localhost:8080

Usuarios de prueba (crearlos por POST /auth/registro):

| Usuario | Rol | Correo |
| --- | --- | --- |
| Contratante A | CONTRATANTE | contratante.a@test.com |
| Contratante B | CONTRATANTE | contratante.b@test.com |
| Trabajador | TRABAJADOR | trabajador@test.com |

Obtener un token (login) y guardarlo en una variable:

```bash
TOKEN_A=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"contratante.a@test.com","password":"<CLAVE_A>"}' | jq -r .token)
```

Crear una solicitud con el Contratante B para usarla en el ataque 4:

```bash
curl -s -X POST $BASE/solicitudes \
  -H "Authorization: Bearer $TOKEN_B" -H "Content-Type: application/json" \
  -d '{"trabajadorId": <ID_TRABAJADOR>, "descripcion": "Solicitud de B"}'
# anotar el id devuelto como ID_SOLICITUD_B
```

---

## Ataque 1 · Acceder a /solicitudes sin token

Riesgo: control de acceso roto (A01). Un recurso protegido no debe responder sin autenticación.

```bash
curl -i $BASE/solicitudes
```

| | |
| --- | --- |
| Esperado | 401 Unauthorized con cuerpo ApiError en JSON (timestamp, status, error, mensaje, ruta) |
| Control | AuthenticationEntryPoint y anyRequest().authenticated() |
| Resultado real | pendiente |
| Evidencia | docs/evidence/ataque1-sin-token.png |

Nota: si responde 403 en lugar de 401, falta el AuthenticationEntryPoint (LAB-39).

---

## Ataque 2 · Token manipulado en jwt.io (cambiar role a ADMIN)

Riesgo: falsificación de identidad o privilegios (A02/A07). Un token alterado no debe ser aceptado.

Pasos:
1. Haz login con el Contratante A y copia el token.
2. Pégalo en jwt.io, cambia el claim role a ADMIN en el payload.
3. Copia el token resultante (la firma ya no corresponde) como TOKEN_MANIPULADO.
4. Úsalo contra un recurso protegido:

```bash
curl -i $BASE/solicitudes -H "Authorization: Bearer <TOKEN_MANIPULADO>"
```

| | |
| --- | --- |
| Esperado | 401 Unauthorized: la firma HS256 no coincide y el filtro limpia el contexto |
| Control | JwtService valida firma, expiración y emisor; JwtAuthenticationFilter |
| Resultado real | pendiente |
| Evidencia | payload decodificado en jwt.io (sin el token completo) y respuesta 401 |

---

## Ataque 4 · Token de un contratante sobre la solicitud de otro

Riesgo: referencia directa insegura a objetos, IDOR (A01). Conocer el id no debe dar acceso.

```bash
# Consultar
curl -i $BASE/solicitudes/<ID_SOLICITUD_B> -H "Authorization: Bearer $TOKEN_A"

# Cancelar
curl -i -X PATCH $BASE/solicitudes/<ID_SOLICITUD_B>/cancelar -H "Authorization: Bearer $TOKEN_A"
```

| | |
| --- | --- |
| Esperado | 404 Not Found en ambos casos (no 403, para no revelar que la solicitud existe) |
| Verificación | la solicitud de B sigue en estado PENDIENTE (consultarla con TOKEN_B) |
| Control | consultas por participante (findByIdAndContratanteId) en SolicitudService |
| Resultado real | pendiente |
| Evidencia | docs/evidence/ataque4-solicitud-ajena.png |

---

## Ataque 7 · Login con correo inexistente vs. clave errónea

Riesgo: enumeración de usuarios (A07). Las dos respuestas deben ser indistinguibles.

```bash
# Correo que no existe
curl -i -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"noexiste@test.com","password":"cualquiera123"}'

# Correo existente, clave incorrecta
curl -i -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"contratante.a@test.com","password":"claveincorrecta1"}'
```

| | |
| --- | --- |
| Esperado | 401 Unauthorized en ambos, con el mismo mensaje en el cuerpo |
| Control | CredencialesInvalidasException con mensaje único (RS-03) |
| Resultado real | pendiente |
| Evidencia | las dos respuestas lado a lado |

---

## Resumen

| # | Ataque | Esperado | Resultado | Evidencia |
| --- | --- | --- | --- | --- |
| 1 | Sin token | 401 | pendiente | pendiente |
| 2 | Token manipulado | 401 | pendiente | pendiente |
| 4 | Solicitud ajena | 404 | pendiente | pendiente |
| 7 | Login: mismo 401 y mensaje | 401 idéntico | pendiente | pendiente |


---

# Ataques 3, 5 y 6 y resultado real del 2

Resultados obtenidos contra la API en ejecución con el perfil dev. Los tokens se muestran como marcadores.
Las respuestas reales están en las capturas de docs/evidence/.

## Ataque 2 · Token con role cambiado a ADMIN (resultado real)

Se hizo login como contratante, se pegó el token en jwt.io, se cambió role a ADMIN y se envió el token resultante.

```powershell
curl.exe -i http://localhost:8080/api/v1/admin/usuarios -H "Authorization: Bearer <TOKEN_MANIPULADO>"
```

| | |
| --- | --- |
| Esperado | 401 Unauthorized (la firma ya no coincide) |
| Resultado real | 401, mensaje "Token inválido o expirado", ruta /api/v1/admin/usuarios (21:11:48) |
| Evidencia | docs/evidence/ataque2-role-alterado-401.png |

## Ataque 3 · Token vencido

Riesgo: sesiones que no caducan (A07). Un token vencido no debe aceptarse.

Se arrancó la app con la duración del token reducida a 1 minuto y se hizo login. La primera llamada con el token respondió 200 (21:15:19). El token venció a las 21:15:45 y la misma llamada, con el mismo token, se repitió a las 21:16:02.

```powershell
curl.exe -i http://localhost:8080/api/v1/usuarios/me -H "Authorization: Bearer <TOKEN_VENCIDO>"
```

| | |
| --- | --- |
| Esperado | 401 Unauthorized |
| Resultado real | 401, mensaje "Token inválido o expirado", ruta /api/v1/usuarios/me (21:16:02). Lo único que cambió entre el 200 y el 401 fue la hora |
| Control | JwtService.validar verifica la expiración y el filtro deja el contexto sin autenticar |
| Evidencia | docs/evidence/ataque3-token-vencido-401.png |

## Ataque 5 · Contratante llama a /admin/usuarios

Riesgo: control de acceso roto (A01). Un rol sin permiso no debe ver datos de contacto.

```powershell
curl.exe -i http://localhost:8080/api/v1/admin/usuarios -H "Authorization: Bearer <TOKEN_CONTRATANTE>"
```

| | |
| --- | --- |
| Esperado | 403 Forbidden |
| Resultado real | 403, mensaje "No tienes permisos para acceder a este recurso", ruta /api/v1/admin/usuarios (21:04:49) |
| Control | regla /api/v1/admin/\*\* solo ADMIN y @PreAuthorize en AdminController |
| Evidencia | docs/evidence/ataque5-contratante-admin-403.png |

## Ataque 6 · Registro con "rol": "ADMIN"

Riesgo: escalada de privilegios (A01). El rol ADMIN no se puede obtener por el registro público.

```powershell
curl.exe -i -X POST http://localhost:8080/api/v1/auth/registro -H "Content-Type: application/json" -d "<JSON_CON_ROL_ADMIN>"
```

| | |
| --- | --- |
| Esperado | 400 Bad Request |
| Resultado real | 400, mensaje "Rol no permitido en el registro", ruta /api/v1/auth/registro (21:01:10), con el formato ApiError y sin traza. La captura solo muestra las cabeceras; el mensaje salió de la terminal de esa misma ejecución |
| Control | el registro solo acepta TRABAJADOR y CONTRATANTE |
| Evidencia | docs/evidence/ataque6-registro-admin-400.png |

## Resumen de estos ataques

| # | Ataque | Esperado | Resultado | Evidencia |
| --- | --- | --- | --- | --- |
| 2 | Token con role ADMIN | 401 | 401 | ataque2-role-alterado-401.png |
| 3 | Token vencido | 401 | 401 | ataque3-token-vencido-401.png |
| 5 | Contratante en admin | 403 | 403 | ataque5-contratante-admin-403.png |
| 6 | Registro con ADMIN | 400 | 400 | ataque6-registro-admin-400.png |
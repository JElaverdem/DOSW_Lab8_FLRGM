# Ataques 1, 2, 4 y 7

Pruebas manuales contra la API en ejecución (PowerShell con `curl.exe`). Cada ataque indica la petición, el
resultado esperado, el resultado real y la evidencia. Los tokens se muestran como variables, nunca completos.

Entorno: la app se ejecutó con el perfil por defecto (sin `dev`), porque el `AdminSeeder` falla al arrancar con
una base de datos nueva (`telefono` es `NOT NULL` y el seeder no lo asigna). Estos tres ataques no usan al `ADMIN`.

## Preparación

Variables base:

```powershell
$BASE = "http://localhost:8080/api/v1"
$clave = "ContrasenaDemo123!"
```

Usuarios de prueba (creados por `POST /auth/registro`):

| Usuario | Rol | Correo |
| --- | --- | --- |
| Contratante A | CONTRATANTE | contratante.a@test.com |
| Contratante B | CONTRATANTE | contratante.b@test.com |
| Trabajador | TRABAJADOR | trabajador@test.com |

Obtener un token (login) y guardarlo en una variable. El campo del token es `accessToken`:

```powershell
function Login($correo) {
  (Invoke-RestMethod -Method Post -Uri "$BASE/auth/login" -ContentType "application/json" `
   -Body (@{correo=$correo;password=$clave} | ConvertTo-Json)).accessToken
}
$TOKEN_A = Login "contratante.a@test.com"
$TOKEN_B = Login "contratante.b@test.com"
```

Crear una solicitud con el Contratante B para usarla en el ataque 4 (en esta ejecución, `id` = 1):

```powershell
$TRAB_ID = (Invoke-RestMethod "$BASE/trabajadores" -Headers @{Authorization="Bearer $TOKEN_B"} |
            Where-Object { $_.nombre -eq "Trabajador Demo" } | Select-Object -First 1).id
$sol = Invoke-RestMethod -Method Post -Uri "$BASE/solicitudes" -Headers @{Authorization="Bearer $TOKEN_B"} `
       -ContentType "application/json" -Body (@{trabajadorId=$TRAB_ID;descripcion="Solicitud de B"} | ConvertTo-Json)
$ID_B = $sol.id
```

---

## Ataque 1 · Acceder a /solicitudes sin token

Riesgo: control de acceso roto (A01). Un recurso protegido no debe responder sin autenticación.

```powershell
curl.exe -i "$BASE/solicitudes"
```

| | |
| --- | --- |
| Esperado | 401 Unauthorized con cuerpo ApiError en JSON (timestamp, status, error, mensaje, ruta) |
| Control | AuthenticationEntryPoint y anyRequest().authenticated() |
| Resultado real | 401, mensaje "Token inválido o expirado", ruta /api/v1/solicitudes (22:58:54), cuerpo ApiError en JSON y sin traza |
| Evidencia | docs/evidence/ataque1-sin-token.png |

---

## Ataque 2 · Token manipulado en jwt.io (cambiar role a ADMIN)

Riesgo: falsificación de identidad o privilegios (A02/A07). Un token alterado no debe ser aceptado.

El resultado real de este ataque está documentado más abajo, en la sección «Ataque 2 · Token con role cambiado a ADMIN (resultado real)»: respondió 401 y la evidencia es `docs/evidence/ataque2-role-alterado-401.png`.

| | |
| --- | --- |
| Esperado | 401 Unauthorized: la firma HS256 no coincide y el filtro limpia el contexto |
| Control | JwtService valida firma, expiración y emisor; JwtAuthenticationFilter |
| Resultado real | 401 (ver sección de resultados reales más abajo) |
| Evidencia | docs/evidence/ataque2-role-alterado-401.png |

---

## Ataque 4 · Token de un contratante sobre la solicitud de otro

Riesgo: referencia directa insegura a objetos, IDOR (A01). Conocer el id no debe dar acceso.

```powershell
# Consultar
curl.exe -i "$BASE/solicitudes/$ID_B" -H "Authorization: Bearer $TOKEN_A"

# Cancelar
curl.exe -i -X PATCH "$BASE/solicitudes/$ID_B/cancelar" -H "Authorization: Bearer $TOKEN_A"

# Verificación con el token del dueño (B)
Invoke-RestMethod "$BASE/solicitudes/$ID_B" -Headers @{Authorization="Bearer $TOKEN_B"} | Format-List
```

| | |
| --- | --- |
| Esperado | 404 Not Found en ambos casos (no 403, para no revelar que la solicitud existe) |
| Resultado real | Consultar: 404, mensaje "Solicitud no encontrada", ruta /api/v1/solicitudes/1 (22:55:54). Cancelar: 404, mismo mensaje, ruta /api/v1/solicitudes/1/cancelar |
| Verificación | Con el token de B la solicitud 1 sigue en estado PENDIENTE: el intento de A no cambió nada |
| Control | consultas por participante (findByIdAndContratanteId) en SolicitudService |
| Evidencia | docs/evidence/ataque4-solicitud-ajena.png |

---

## Ataque 7 · Login con correo inexistente vs. clave errónea

Riesgo: enumeración de usuarios (A07). Las dos respuestas deben ser indistinguibles.

```powershell
'{"correo":"noexiste@test.com","password":"cualquiera123"}' | Set-Content -Encoding ascii "$env:TEMP\a7-1.json"
'{"correo":"contratante.a@test.com","password":"claveincorrecta1"}' | Set-Content -Encoding ascii "$env:TEMP\a7-2.json"

# Correo que no existe
curl.exe -i -X POST "$BASE/auth/login" -H "Content-Type: application/json" --data-binary "@$env:TEMP\a7-1.json"

# Correo existente, clave incorrecta
curl.exe -i -X POST "$BASE/auth/login" -H "Content-Type: application/json" --data-binary "@$env:TEMP\a7-2.json"
```

| | |
| --- | --- |
| Esperado | 401 Unauthorized en ambos, con el mismo mensaje en el cuerpo |
| Resultado real | Ambos 401, mensaje "Credenciales inválidas", ruta /api/v1/auth/login (22:56:32). Los cuerpos solo difieren en el timestamp |
| Control | CredencialesInvalidasException con mensaje único (RS-03) |
| Evidencia | docs/evidence/ataque7-login-mismo-401.png |

---

## Resumen

| # | Ataque | Esperado | Resultado | Evidencia |
| --- | --- | --- | --- | --- |
| 1 | Sin token | 401 | 401 | ataque1-sin-token.png |
| 2 | Token manipulado | 401 | 401 (ver sección de resultados reales) | ataque2-role-alterado-401.png |
| 4 | Solicitud ajena | 404 | 404 en consultar y cancelar; la solicitud sigue PENDIENTE | ataque4-solicitud-ajena.png |
| 7 | Login: mismo 401 y mensaje | 401 idéntico | 401 idéntico ("Credenciales inválidas") | ataque7-login-mismo-401.png |


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
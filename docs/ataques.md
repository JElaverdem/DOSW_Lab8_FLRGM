# Ataques 1, 2, 4 y 7 · OficioYa (LAB-56)

Pruebas manuales contra la API en ejecución. Cada ataque indica la petición, el resultado esperado
y un espacio para el resultado real y la evidencia.


## Preparación

```bash
BASE=http://localhost:8080        # ajustar si el contrato usa /api/v1
```

Usuarios de prueba (crearlos por `POST /auth/registro`):

| Usuario | Rol | Correo |
| --- | --- | --- |
| Contratante A | `CONTRATANTE` | `contratante.a@test.com` |
| Contratante B | `CONTRATANTE` | `contratante.b@test.com` |
| Trabajador | `TRABAJADOR` | `trabajador@test.com` |

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

## Ataque 1 · Acceder a `/solicitudes` sin token

**Riesgo:** control de acceso roto (A01). Un recurso protegido no debe responder sin autenticación.

```bash
curl -i $BASE/solicitudes
```

| | |
| --- | --- |
| Esperado | `401 Unauthorized` con cuerpo `ApiError` en JSON (`timestamp`, `status`, `error`, `mensaje`, `ruta`) |
| Control | `AuthenticationEntryPoint` y `anyRequest().authenticated()` |
| Resultado real | _pendiente_ |
| Evidencia | `docs/evidence/ataque1-sin-token.png` |

Nota: si responde `403` en lugar de `401`, falta el `AuthenticationEntryPoint` (LAB-39).

---

## Ataque 2 · Token manipulado en jwt.io (cambiar `role` a `ADMIN`)

**Riesgo:** falsificación de identidad o privilegios (A02/A07). Un token alterado no debe ser aceptado.

Pasos:
1. Haz login con el Contratante A y copia el token.
2. Pégalo en <https://jwt.io>, cambia el claim `role` a `ADMIN` en el payload.
3. Copia el token resultante (la firma ya no corresponde) como `TOKEN_MANIPULADO`.
4. Úsalo contra un recurso protegido:

```bash
curl -i $BASE/solicitudes -H "Authorization: Bearer <TOKEN_MANIPULADO>"
```

| | |
| --- | --- |
| Esperado | `401 Unauthorized`: la firma HS256 no coincide y el filtro limpia el contexto |
| Control | `JwtService` valida firma, expiración y emisor; `JwtAuthenticationFilter` |
| Resultado real | _pendiente_ |
| Evidencia | payload decodificado en jwt.io (sin el token completo) y respuesta 401 |

---

## Ataque 4 · Token de un contratante sobre la solicitud de otro

**Riesgo:** referencia directa insegura a objetos, IDOR (A01). Conocer el id no debe dar acceso.

```bash
# Consultar
curl -i $BASE/solicitudes/<ID_SOLICITUD_B> -H "Authorization: Bearer $TOKEN_A"

# Cancelar
curl -i -X PATCH $BASE/solicitudes/<ID_SOLICITUD_B>/cancelar -H "Authorization: Bearer $TOKEN_A"
```

| | |
| --- | --- |
| Esperado | `404 Not Found` en ambos casos (no `403`, para no revelar que la solicitud existe) |
| Verificación | la solicitud de B sigue en estado `PENDIENTE` (consultarla con `TOKEN_B`) |
| Control | consultas por participante (`findByIdAndContratanteId`) en `SolicitudService` |
| Resultado real | _pendiente_ |
| Evidencia | `docs/evidence/ataque4-solicitud-ajena.png` |

---

## Ataque 7 · Login con correo inexistente vs. clave errónea

**Riesgo:** enumeración de usuarios (A07). Las dos respuestas deben ser indistinguibles.

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
| Esperado | `401 Unauthorized` en ambos, con **el mismo mensaje** en el cuerpo |
| Control | `CredencialesInvalidasException` con mensaje único (RS-03) |
| Resultado real | _pendiente_ |
| Evidencia | las dos respuestas lado a lado |

---

## Resumen

| # | Ataque | Esperado | Resultado | Evidencia |
| --- | --- | --- | --- | --- |
| 1 | Sin token | 401 | _pendiente_ | _pendiente_ |
| 2 | Token manipulado | 401 | _pendiente_ | _pendiente_ |
| 4 | Solicitud ajena | 404 | _pendiente_ | _pendiente_ |
| 7 | Login: mismo 401 y mensaje | 401 idéntico | _pendiente_ | _pendiente_ |

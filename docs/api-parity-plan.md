# Plan: paridad de la API `chaoticteams/backend` (Spring Boot) con la doc de go-server

> **Estado: implementado y verificado** (rama `feat/api-parity`). Los 30 endpoints de la doc responden con los contratos de go-server
> (`docs/smoke-test.sh` los recorre) y `/v3/api-docs` los publica todos. Desviaciones respecto al plan original:
> - CORS usa `app.cors.allowed-origin-patterns` (env `CORS_ALLOWED_ORIGINS`, por defecto `*`) en lugar de leer la tabla `site`, porque con esa tabla un origen nuevo nunca podría crear su primer comentario.
> - La cookie `access_token` dura 10 h (igual que el JWT, no 1 h) y su `Secure`/`SameSite` se configuran con `COOKIE_SECURE` / `COOKIE_SAME_SITE`.
> - Los listados de usuario inexistente devuelven `[]` (go devolvía 500 en courses) y los recursos ajenos devuelven 404 como en go.
> - Los tests automáticos se añadieron en un segundo paso (la primera entrega los omitió por error). Ver "Tests automáticos" abajo.
> - Swagger se escribió junto con cada controlador, no en una fase aparte.

## Contexto
Buscamos que el backend Java (`~/projects/backend/backend`, Spring Boot 3.4 + springdoc 2.8.6 + JPA/Postgres + JWT) exponga los mismos endpoints y contratos que la doc de Postman https://al3xdiaz.github.io/go-server/. Los contratos de respuesta, las reglas de auth y los status codes salen del código fuente de `github.com/Al3xDiaz/go-server`, porque la doc solo muestra requests. El frontend `~/projects/frontend/dashboard` ya consume esos contratos.

Decisiones del usuario:
- Mantener el prefijo `/api`.
- Validar rutas **y** contratos.
- Guardar el plan como archivo en el repo.
- Conservar los extras (`/health`, `/auth/refresh`).

**Remotes git: ya están correctos.** `origin` hace fetch de gitlab y push a gitlab y github. No hay que tocarlos.

## Resultado de la validación

Leyenda: ✅ existe y cumple · ⚠️ existe con diferencias de contrato · ❌ falta · ➕ extra.

| Doc (go-server) | Java hoy | Estado / nota |
|---|---|---|
| POST /auth/signup | /api/auth/signup | ⚠️ la doc manda `userName` y el DTO espera `username`. Responde `{username,token,refreshToken}`, pero go y el frontend esperan `{user, token}` |
| POST /auth/login | /api/auth/login | ⚠️ la request cumple. La respuesta no trae `user` ni pone la cookie `access_token` |
| GET /auth/userdata | /api/auth/userdata | ⚠️ serializa `UserEntity` directamente (`username`, `profileEntity`, `roleEntity`). Go devuelve `userName`, `profile{…, telephone[]}` |
| POST /auth/validatecredetial | /api/auth/validatecredetial | ⚠️ go responde 204 y pone la cookie. Java devuelve el usuario |
| (no está en la doc; lo usa el frontend) DELETE /auth/logout | — | ➕ agregarlo (limpia la cookie, responde 204) |
| GET /commentaries | /api/commentaries | ⚠️ **bug**: filtra por `request.getRequestURL()`, que es la URL del backend, cuando debería usar el header `Origin`. Además hoy exige auth y en go es público |
| POST /commentaries, GET /commentaries/{id}, DELETE /commentaries/{id} | — | ❌ |
| GET /courses?username=&limit= ; POST /courses[?type=bulk] ; PATCH/DELETE /courses/{id} | — | ❌ (go también tiene GET /courses/{id}) |
| GET /achievements?username= ; POST [?type=bulk] ; PATCH/DELETE /{id} | — | ❌ |
| GET /vcard/{username} | — | ❌ (responde texto vCard como adjunto `.vcf`) |
| GET /profile?username= ; PATCH /profile | — | ❌ (el PATCH de la doc usa `first_name`/`last_name`) |
| POST /telephone ; DELETE /telephone/{id} | — | ❌ (la entidad existe y el repo no) |
| GET /users | — | ❌ (en go devuelve `string[]` de usernames y es público) |
| GET /projects?username= ; POST ; PATCH/DELETE /{id} | — | ❌ (go también tiene GET /projects/{id}) |
| GET /galleries?username= ; POST ; DELETE /{id} | — | ❌ |
| GET /version | /api/version | ✅ |
| — | /api/health, /api/auth/refresh | ➕ se conservan |

Swagger: solo documenta lo que existe y tiene errores:
- En login y signup, los campos del body aparecen como `@Parameter` (query).
- Commentaries declara un parámetro inexistente `requestURL`.
- El server está en `localhost:8080` y los tags no son consistentes.

## Contratos objetivo (sacados del código go)
- **Entidades nuevas** (JPA, `ddl-auto=update` las crea). Todas con `id` y `user` en `@ManyToOne`, anotado `@JsonIgnore`:
  - `CourseEntity{name,image}`
  - `AchievementEntity{year>0,comment,title}`
  - `ProjectEntity{title,description,image,url,startDate(not null),endDate}`
  - `GalleryEntity{image}`
- **Listados públicos** con `?username=` y `?limit=` opcional (-1 = sin límite):
  - achievements: `year desc`
  - projects: `startDate desc`
  - courses y galleries: sin orden.
- **Create**: 200 con la entidad creada. Con `?type=bulk` (solo courses y achievements) recibe un array y responde **204**.
- **Update (PATCH)**: merge parcial solo de los campos presentes. Busca por `id` **y** que el dueño sea el usuario del JWT. Si no es suyo, **404**. Si se aplica, 200 con la entidad.
- **Delete**: mismo chequeo de dueño y 404, y responde **204**.
- **Auth**:
  - login y signup responden `{user: UserResponse, token, refreshToken}`. Es un superset de go, así que el frontend sigue funcionando.
  - Ponen la cookie `access_token` (HttpOnly, Secure, Lax, 1h).
  - signup acepta `userName` y `username` (`@JsonAlias`).
- **UserResponse DTO**: `{id,userName,email,verified,profile:{…todos los campos…, telephone:[{id,phoneNumber,countryCode,whatsapp}]}}`. Se usa en userdata, GET/PATCH profile, login y signup.
- **PATCH /profile**: acepta camelCase y snake_case (`first_name`/`last_name` vía `@JsonAlias`) y hace merge parcial.
- **Commentaries**:
  - Toman el sitio del header `Origin`. En create se hace find-or-create de `SiteEntity`.
  - Respuesta `{id,userId,comment,site}`.
  - Delete solo si el comentario es del usuario.
- **vcard**:
  - `Content-Type: text/vcard` y `Content-Disposition: attachment; filename=<user>.vcf`.
  - Líneas VERSION 2.1 como en go: N, FN, NICKNAME, TEL (primer teléfono), EMAIL, PHOTO, X-SOCIALPROFILE…, URL.
  - 404 si no hay usuario o si no tiene teléfono.
- **Errores**: body `{"error": msg}` como en go: 404 NotFound y 400 BadRequest.

## Implementación (por fases, un commit por fase)

**Fase 0: base compartida**
- `utils/NotFoundException` y un handler en `utils/GlobalErrorHandler.java` que devuelve 404 `{"error":…}`.
- `auth/services/CurrentUserService`: obtiene el `UserEntity` a partir de `SecurityContextHolder` y reutiliza `UserRepository.findByUsername`.
- `SecurityConfig.java`:
  - Aceptar el token también desde la cookie `access_token` con un `BearerTokenResolver` custom (header primero y cookie después).
  - `permitAll` con `HttpMethod.GET` para `/api/commentaries`, `/api/courses`, `/api/achievements`, `/api/projects`, `/api/galleries`, `/api/profile`, `/api/users`, `/api/vcard/**` y `/swagger-ui/**`.
  - CORS con `allowCredentials` y orígenes tomados de la tabla `site`, igual que go.

**Fase 1: arreglar lo existente (⚠️)**
- `auth/dto/UserResponse`, `ProfileResponse`, `TelephoneResponse` con mapper estático `from(UserEntity)`.
- `AuthController`:
  - Nuevo `LoginResponse{user,token,refreshToken}` en lugar de `AuthenticationResponse`.
  - Poner la cookie.
  - `validatecredetial` → 204 + cookie.
  - Nuevo `DELETE /logout`.
- `AuthenticationSignUpRequest.username` con `@JsonAlias("userName")`.
- `CommentaryController.list`: usar `@RequestHeader("Origin")` y dejarlo público.

**Fase 2: módulos nuevos.** Mismo patrón que `commentaries/`: `controllers/ entities/ repository/ services/ dto/`. Paquetes nuevos:
- `courses/`, `achievements/`, `projects/`, `galleries/`: CRUD con los contratos de arriba. Los repos usan queries derivadas: `findByUserUsername(String, Pageable/Sort)` y `findByIdAndUserUsername(Long,String)`.
- `profile/`:
  - `ProfileController`: `GET /api/profile?username=` y `PATCH /api/profile` con `ProfileUpdateRequest` y alias snake_case.
  - `TelephoneController`: `POST /api/telephone` y `DELETE /api/telephone/{id}`, más un nuevo `TelephoneRepository`.
  - `VCardController`: `GET /api/vcard/{username}`.
  - `UsersController`: `GET /api/users` → `List<String>`, reutilizando `UserService.listUsers()`.
- Commentaries: `POST`, `GET /{id}`, `DELETE /{id}` en `CommentaryController` y `CommentariesService`.

**Fase 3: Swagger con el mismo estilo de la doc**
- Un `@Tag` por módulo con los nombres de la doc: Auth, Commentaries, Courses, Achievements, Profile, Projects, Galleries, Config.
- Cada operación con `@Operation(summary=<nombre de la doc: list/create/create bulk/update/delete/detail…>)`.
- `@io.swagger.v3.oas.annotations.parameters.RequestBody` con `@ExampleObject` que copia el ejemplo exacto de la doc (por ejemplo el body de signup y el array bulk).
- `@Parameter(in=QUERY)` para `username`, `limit` y `type`.
- `@Parameter(in=HEADER, name="Origin")` en commentaries.
- `@ApiResponse` con 200, 204, 401 y 404.
- `@SecurityRequirements()` vacío en los endpoints públicos.
- Quitar los `@Parameter` erróneos de login y signup.
- Agregar el server `http://localhost:8000` junto al 8080 actual, o usar una variable.

**Fase 4: documento en el repo**
- Guardar esta validación y el plan en `docs/api-parity-plan.md` dentro del backend, con la tabla de estado y un checklist por endpoint.
- Este archivo se crea primero, antes de la Fase 0, para que quede versionado.

## Archivos clave
- Modificar:
  - `src/main/java/com/chaoticteam/backend/configuration/SecurityConfig.java`
  - `configuration/SwaggerConfig.java`
  - `auth/controllers/AuthController.java`
  - `auth/dto/AuthenticationSignUpRequest.java`
  - `commentaries/**`
  - `utils/GlobalErrorHandler.java`
- Crear: los paquetes `courses/`, `achievements/`, `projects/`, `galleries/` y `profile/`, `auth/dto/UserResponse.java` y `docs/api-parity-plan.md`.

## Tests automáticos

69 tests de aceptación (JUnit 5 + MockMvc) que ejercitan toda la app (seguridad, controladores, JPA) contra un **PostgreSQL real y desechable**:

```bash
scripts/test.sh                              # levanta Postgres en Docker, corre Maven en un contenedor y limpia
scripts/test.sh -Dtest=AuthAcceptanceTest    # una sola clase
```

Solo requiere Docker (sin JDK local ni compose). Si tienes JDK, puedes usar `./mvnw verify` con un Postgres en `localhost:55432` (o variables `TEST_DB_*`, ver `src/test/resources/application-test.properties`).

| Clase | Qué valida |
|---|---|
| `AuthAcceptanceTest` | signup/login (`userName` alias, `{user, token, refreshToken}`, cookie), 401, userdata por Bearer o cookie, cookie vencida en endpoints públicos, validatecredetial, logout, refresh |
| `OwnedResourcesAcceptanceTest` | courses, achievements, projects y galleries: listado público por `username`/`limit`, escritura solo del dueño, 404 en datos ajenos, PATCH parcial, 204 al borrar, `?type=bulk`, orden (año/fecha desc), validaciones |
| `ProfileAcceptanceTest` | PATCH snake/camelCase, perfil público sin secretos, teléfonos, vCard (cabeceras y líneas), users |
| `CommentariesAcceptanceTest` | aislamiento por header `Origin`, auth, borrado solo del autor |
| `ApiDocumentationContractTest` | Swagger documenta los 30 endpoints de la doc, tags por módulo, sin `@Parameter` erróneos |

Se comprobó que detectan fallos: al romper a propósito el chequeo de dueño, 4 tests fallan (`expected 404 but was 204`).

## Verificación
1. `./mvnw clean verify` compila y pasa los tests.
2. Se agregan tests `@WebMvcTest`/`@SpringBootTest` + MockMvc por controlador que cubren:
   - Status codes: 200, 204, 401 y el 404 cuando el recurso es de otro usuario.
   - `?type=bulk`.
   - Alias `userName` y `first_name`.
   - Formato vCard.
3. Levantar el stack con `docker compose up` y hacer un smoke test con curl que recorra los ~38 requests de la doc (con prefijo `/api`), usando el token del login y la cookie.
4. Abrir `/swagger-ui/index.html`:
   - Todos los endpoints de la tabla aparecen agrupados por tag.
   - Los ejemplos coinciden con la doc.
   - Un script compara `/v3/api-docs` con la lista de rutas de la doc, y no debe faltar ninguna.
5. Opcional: apuntar el dashboard (`API_URL` a `/api`) y probar login, profile, courses, galleries y projects.

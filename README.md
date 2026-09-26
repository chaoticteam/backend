# Chaotic Team Backend

[![pipeline status](https://gitlab.com/chaoticteams/backend/badges/main/pipeline.svg)](https://gitlab.com/chaoticteams/backend/-/pipelines)
[![coverage report](https://gitlab.com/chaoticteams/backend/badges/main/coverage.svg)](https://gitlab.com/chaoticteams/backend/-/pipelines)
[![license: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

API REST de los sitios `*.chaoticteam.com` (portafolio, cursos, proyectos, comentarios…). Está hecha con
**Spring Boot 3.4**, **Java 17** y **PostgreSQL**, y reemplaza al servidor original en Go
([go-server](https://al3xdiaz.github.io/go-server/)) manteniendo sus mismos endpoints y contratos, con el prefijo `/api`.

> El repositorio principal está en [GitLab](https://gitlab.com/chaoticteams/backend) y se espeja en
> [GitHub](https://github.com/chaoticteam/backend).

## Endpoints

Todas las rutas cuelgan de `/api`. Los endpoints marcados como **público** no requieren autenticación; el resto
necesita un JWT (cabecera `Authorization: Bearer <token>` o cookie `access_token`).

| Módulo | Endpoints | Acceso |
|---|---|---|
| Auth | `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh` | público |
| | `GET /auth/userdata`, `POST /auth/validatecredetial`, `DELETE /auth/logout` | autenticado |
| Comentarios | `GET /commentaries` (por cabecera `Origin`) | público |
| | `POST /commentaries`, `GET`/`DELETE /commentaries/{id}` | autenticado |
| Cursos, logros, proyectos, galerías | `GET /courses`, `/achievements`, `/projects`, `/galleries` (`?username=&limit=`) | público |
| | `POST` (con `?type=bulk` en cursos y logros), `PATCH`/`DELETE /{id}`, `GET /{id}` en cursos y proyectos | autenticado, solo el dueño |
| Perfil | `GET /profile?username=`, `GET /vcard/{username}`, `GET /users` | público |
| | `PATCH /profile`, `POST /telephone`, `DELETE /telephone/{id}` | autenticado |
| Config | `GET /version`, `GET /health` | público |

La referencia completa (esquemas, ejemplos y códigos de respuesta) está en Swagger.

## Inicio rápido

Solo necesitas Docker con Compose.

```bash
cp .env.example .env        # y define JWT_SECRET (openssl rand -hex 32)
docker compose up -d
```

- API: <http://localhost:8080/api/version>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html> (OpenAPI en `/v3/api-docs`)
- PostgreSQL corre en el servicio `db` del compose, con un volumen persistente.

El servicio `backend` monta `./src` y usa `spring-boot:run` con DevTools, así que recarga al cambiar el código.
Para usar una base de datos externa, define `SPRING_DATASOURCE_DB_HOST` (y las demás `SPRING_DATASOURCE_*`) en `.env`.

## Configuración

| Variable | Requerida | Por defecto | Descripción |
|---|---|---|---|
| `JWT_SECRET` | sí | — | Clave HMAC con la que se firman los JWT (mínimo 32 caracteres) |
| `SPRING_DATASOURCE_DB_HOST` / `_DB_PORT` / `_DB_NAME` | no | `db` / `5432` / `chaoticteamdb` | Conexión a PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | no | `user` / `password` | Credenciales de PostgreSQL |
| `COOKIE_SECURE` | no | `true` (`false` en el compose) | Marca `Secure` de la cookie `access_token`; usa `true` detrás de HTTPS |
| `COOKIE_SAME_SITE` | no | `Lax` | `SameSite` de la cookie |
| `CORS_ALLOWED_ORIGINS` | no | `*` | Orígenes permitidos, separados por coma |
| `VERSION` | no | `1.0` | `GET /api/version` responde `v<VERSION>` |

Los tokens de acceso duran 10 h y los de refresco 7 días (valores fijos en `JwtService`).
El esquema de la base de datos lo crea Hibernate al arrancar (`ddl-auto=update`).

## Pruebas y cobertura

Los tests son de aceptación (JUnit 5 + MockMvc) y corren contra un PostgreSQL real y desechable.
El script levanta la base de datos en Docker, ejecuta Maven en un contenedor y limpia al terminar; no necesitas JDK local.

```bash
scripts/test.sh                              # toda la suite + porcentaje de cobertura
scripts/test.sh -Dtest=AuthAcceptanceTest    # una sola clase
```

El reporte de cobertura (JaCoCo) queda en `target/site/jacoco/index.html`.
`scripts/coverage.sh` imprime el porcentaje total de líneas (`Total coverage: NN.N %`), que es la línea que la
pipeline lee para el badge de cobertura.

Con un JDK 17 local también puedes usar `./mvnw verify` teniendo un PostgreSQL en `localhost:55432`
(o las variables `TEST_DB_*`, ver `src/test/resources/application-test.properties`).
`docs/smoke-test.sh` recorre con `curl` todos los endpoints contra una instancia en ejecución.

## Estructura

```
src/main/java/com/chaoticteam/backend/
├── auth/            # signup, login, JWT, usuarios y perfil
├── commentaries/    # comentarios por sitio
├── courses/ achievements/ projects/ galleries/   # recursos del usuario
├── profile/         # perfil, teléfonos, vCard y lista de usuarios
├── common/          # base compartida de los recursos con dueño
├── configuration/   # seguridad (JWT/CORS) y OpenAPI
└── utils/           # manejo de errores, merge parcial de JSON, paginación
docs/                # plan de paridad con go-server y smoke test
scripts/             # test.sh y coverage.sh
.gitlab-ci.yml       # pipeline de CI/CD (GitLab)
GitVersion.yml       # cálculo de versiones (GitVersion 6)
```

## CI/CD

La pipeline vive en [`.gitlab-ci.yml`](.gitlab-ci.yml) y corre **solo** en Merge Requests hacia `main` y en `main`
(no hay pipelines por push a ramas sueltas).

| Stage | Job | Qué hace |
|---|---|---|
| `version` | `get_version` | Calcula la versión con GitVersion (`GitVersion.yml`) |
| `build` | `build` | Compila el `.jar` con la versión calculada |
| | `build_image` | Construye y publica la imagen en el Container Registry de GitLab |
| `test-coverage` | `test-coverage` | Corre los tests contra un PostgreSQL de servicio, publica el reporte JUnit y la cobertura (badge) |
| `docs` | `openapi_spec`, `openapi_lint`, `api_docs`, `pages` | Exporta el OpenAPI, lo valida y publica la referencia de la API (Scalar) |
| `destroy` | `stop_environment` | Cierra el ambiente `review-<id>` al mergear o cerrar el MR |

**Imágenes Docker.** Se publican en cada ejecución; la etiqueta lleva la versión de GitVersion y un sufijo según el ambiente:

| Ambiente | Etiquetas | Ejemplo |
|---|---|---|
| producción (`main`) | `<versión>` y `latest` | `0.1.0-18`, `latest` |
| review (MR) | `<versión>-review-<id del MR>` y `review-<id>` | `0.1.0-feat-x.1-26-review-12`, `review-12` |

La versión también queda dentro de la imagen: `GET /api/version` responde `v<etiqueta>`.
GitVersion parte de `next-version: 0.1.0`; al crear el primer tag semver (por ejemplo `git tag v0.1.0`) se puede quitar esa línea.

**Documentación de la API.** En cada MR aparece el enlace «API docs» (artefacto) y en `main` se publica en GitLab Pages.
Los servidores de la referencia apuntan al ambiente de la pipeline (`api.chaoticteam.com` en producción).

**Ambientes.** Por ahora no hay despliegue: producción es `https://api.chaoticteam.com` y cada MR abre un ambiente
`review-<id>` solo para trazabilidad. Cuando exista un destino se añade un stage `deploy`.

## Contribuir

`main` está protegida: los cambios entran únicamente mediante Merge Request (Pull Request en GitHub), nunca con push directo.

1. Crea una rama desde `main` y haz tus cambios.
2. Ejecuta `scripts/test.sh`; la suite debe pasar.
3. Si cambias la API, actualiza los ejemplos y anotaciones de Swagger; hay un test que verifica que los endpoints
   de la documentación sigan publicados.
4. Abre un Merge Request hacia `main` (se rellena solo con la plantilla) y espera a que la pipeline pase.

## Licencia

Distribuido bajo la licencia [MIT](LICENSE). Copyright (c) 2025 Alex Diaz.

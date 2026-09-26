## Resumen

<!-- Qué cambia y por qué. Enlaza el issue si existe: Closes #123 -->

## Tipo de cambio

- [ ] Nueva funcionalidad (`feat`)
- [ ] Corrección de bug (`fix`)
- [ ] Refactor / mantenimiento (`refactor`, `chore`)
- [ ] Documentación / CI (`docs`, `ci`)
- [ ] Cambio incompatible en la API (breaking change)

## Cómo se probó

<!-- Tests nuevos o ajustados, y qué se verificó a mano. -->

- [ ] `scripts/test.sh` pasa en local
- [ ] Añadí o ajusté tests de aceptación para el comportamiento nuevo

## Impacto en la API

- [ ] No cambia endpoints ni contratos
- [ ] Cambia endpoints o contratos: Swagger/OpenAPI actualizado (anotaciones y ejemplos)
- [ ] Añade o cambia variables de entorno: `.env.example` y README actualizados

<!-- Si es un breaking change, describe la migración para el dashboard u otros clientes. -->

## Checklist

- [ ] El título sigue el formato de commits (`feat: ...`, `fix: ...`)
- [ ] La pipeline pasa (build, test-coverage y docs)
- [ ] Sin secretos ni credenciales en el código o los logs

#!/usr/bin/env bash
# Runs the automated tests against a throwaway PostgreSQL container.
# Needs only Docker (no local JDK, no compose): the DB and Maven both run in containers.
# Extra arguments go to Maven, e.g.  scripts/test.sh -Dtest=AuthAcceptanceTest
set -euo pipefail
cd "$(dirname "$0")/.."

DB=chaoticteam-db-test
PORT=${TEST_DB_PORT:-55432}   # must match TEST_DB_PORT in src/test/resources/application-test.properties

docker rm -f "$DB" >/dev/null 2>&1 || true
docker run -d --name "$DB" -p "$PORT":5432 --tmpfs /var/lib/postgresql \
  -e POSTGRES_DB=chaoticteam_test -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password \
  postgres:alpine >/dev/null
trap 'docker rm -f "$DB" >/dev/null 2>&1 || true' EXIT

# TCP check: during initdb the temporary server only listens on the unix socket
for _ in $(seq 1 60); do
  if docker exec "$DB" pg_isready -h 127.0.0.1 -U user -d chaoticteam_test >/dev/null 2>&1; then ready=1; break; fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$DB")" != "true" ]; then break; fi
  sleep 1
done
if [ -z "${ready:-}" ]; then
  echo "PostgreSQL did not start:" >&2
  docker logs --tail 20 "$DB" >&2
  exit 1
fi

docker run --rm --network host \
  -e TEST_DB_PORT="$PORT" \
  -v "$PWD":/app -v m2cache:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-17 mvn -B verify "$@"

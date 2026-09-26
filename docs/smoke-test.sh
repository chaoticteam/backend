#!/usr/bin/env bash
# Smoke test of every endpoint in https://al3xdiaz.github.io/go-server/ against this API.
# Needs a running backend with an EMPTY database (it signs up user "admin") and COOKIE_SECURE=false.
# Usage: BASE_URL=http://localhost:8080 bash docs/smoke-test.sh
B=${BASE_URL:-http://localhost:8080}/api; O="Origin: http://localhost:3000"; J="Content-Type: application/json"
CJ=$(mktemp)
req() { # name method path [body] [extra curl args...]
  local name=$1 m=$2 p=$3 body=$4; shift 4
  local out; out=$(curl -s -o /tmp/body.$$ -w "%{http_code}" -X $m "$B$p" -H "$J" -H "$O" "$@" ${body:+-d "$body"})
  printf "%-34s %-6s %-32s -> %s  %s\n" "$name" "$m" "$p" "$out" "$(head -c 110 /tmp/body.$$ | tr '\n\r' '  ')"
}
req "signup (userName alias)" POST /auth/signup '{"userName":"admin","password":"password","email":"admin@example.io","profile":{"firstName":"alex","lastName":"diaz"}}' -c $CJ
req "signup duplicate" POST /auth/signup '{"userName":"admin","password":"password","email":"admin@example.io","profile":{"firstName":"a","lastName":"b"}}'
TOKEN=$(curl -s -X POST $B/auth/login -H "$J" -d '{"username":"admin","password":"password"}' -c $CJ | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['token'])")
echo "login token len: ${#TOKEN}; cookie jar has access_token: $(grep -c access_token $CJ)"
req "login bad password" POST /auth/login '{"username":"admin","password":"nope"}'
A="Authorization: Bearer $TOKEN"
req "userdata (bearer)" GET /auth/userdata "" -H "$A"
req "userdata (cookie only)" GET /auth/userdata "" -b $CJ
req "userdata (no auth)" GET /auth/userdata ""
req "validatecredetial" POST /auth/validatecredetial "" -H "$A"
req "courses create" POST /courses '{"name":"Docker","image":"http://example.com/course-docker.jpg"}' -H "$A"
req "courses create bulk" POST "/courses?type=bulk" '[{"name":"Jenkins","image":"x"},{"name":"Node.js","image":"y"}]' -H "$A"
req "courses list (public)" GET "/courses?username=admin" ""
req "courses list limit=1" GET "/courses?username=admin&limit=1" ""
req "courses detail" GET /courses/1 "" -H "$A"
req "courses patch" PATCH /courses/1 '{"name":"Docker expert"}' -H "$A"
req "courses delete" DELETE /courses/3 "" -H "$A"
req "courses delete again" DELETE /courses/3 "" -H "$A"
req "achievements create" POST /achievements '{"year":2023,"comment":"job DevOps","title":"Abstract"}' -H "$A"
req "achievements bulk" POST "/achievements?type=bulk" '[{"year":2021,"comment":"c","title":"SignsCloud"},{"year":2019,"comment":"d","title":"BIDSS"}]' -H "$A"
req "achievements list (year desc)" GET "/achievements?username=admin" ""
req "achievements patch (doc body)" PATCH /achievements/1 '{"name":"Docker expert"}' -H "$A"
req "achievements invalid year" POST /achievements '{"year":0,"comment":"c","title":"t"}' -H "$A"
req "achievements delete" DELETE /achievements/2 "" -H "$A"
req "projects create" POST /projects '{"title":"portfolio","description":"golang and react","startDate":"2024-06-21T09:49:48.385Z"}' -H "$A"
req "projects list" GET "/projects?username=admin" ""
req "projects patch" PATCH /projects/1 '{"url":"http://example.com/assets/image.jpg"}' -H "$A"
req "projects detail" GET /projects/1 "" -H "$A"
req "projects delete" DELETE /projects/1 "" -H "$A"
req "galleries create" POST /galleries '{"image":"http://example.com/assets/image_1.jpg"}' -H "$A"
req "galleries list" GET "/galleries?username=admin" ""
req "galleries delete" DELETE /galleries/1 "" -H "$A"
req "telephone create" POST /telephone '{"phoneNumber":"87654321","countryCode":"504","whatsapp":true}' -H "$A"
req "profile patch (snake_case)" PATCH /profile '{"first_name":"Alex","last_name":"Diaz","bio":"hi","telephone":null,"twitter":"al3x"}' -H "$A"
req "profile get (public)" GET "/profile?username=admin" ""
req "vcard" GET /vcard/admin ""
curl -s -D - -o /dev/null $B/vcard/admin | grep -iE "content-(type|disposition)"
req "vcard unknown" GET /vcard/nobody ""
req "users" GET /users ""
req "commentaries create" POST /commentaries '{"comment":"Lorem ipsum"}' -H "$A"
req "commentaries list (Origin)" GET /commentaries ""
req "commentaries list other Origin" GET /commentaries "" -H "Origin: http://other.io"
req "commentaries detail" GET /commentaries/1 "" -H "$A"
req "commentaries delete" DELETE /commentaries/1 "" -H "$A"
req "telephone delete" DELETE /telephone/1 "" -H "$A"
req "logout" DELETE /auth/logout "" -H "$A"
req "version" GET /version ""
req "health" GET /health ""
req "public GET with stale cookie" GET "/courses?username=admin" "" -H "Cookie: access_token=garbage"
req "swagger api-docs" GET /../v3/api-docs ""

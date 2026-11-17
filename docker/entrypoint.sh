#!/usr/bin/env bash
# Sobe o PostgreSQL 18 (só em localhost) e, em seguida, a aplicação. Se um dos dois parar, o contêiner para.
# Com DB_URL definido, usa um banco externo e não inicia o PostgreSQL embutido.
set -euo pipefail

log() { echo "[odonto] $*"; }

PG_PID=""
APP_PID=""

encerrar() {
    local codigo="${1:-0}"
    log "Encerrando..."
    [[ -n "$APP_PID" ]] && kill -TERM "$APP_PID" 2>/dev/null && wait "$APP_PID" 2>/dev/null || true
    [[ -n "$PG_PID" ]] && kill -TERM "$PG_PID" 2>/dev/null && wait "$PG_PID" 2>/dev/null || true
    exit "$codigo"
}
trap encerrar SIGTERM SIGINT

# Transcrição de áudio é opcional: sem chave da OpenAI, desliga o provedor e a conversa pede texto.
if [[ -z "${TRANSCRICAO_PROVEDOR:-}" ]]; then
    if [[ -n "${OPENAI_API_KEY:-}" ]]; then export TRANSCRICAO_PROVEDOR=openai; else export TRANSCRICAO_PROVEDOR=none; fi
fi

if [[ -z "${DB_URL:-}" ]]; then
    export POSTGRES_PASSWORD="${DB_SENHA:-odonto}"
    export DB_USUARIO="${POSTGRES_USER:-odonto}"
    export DB_SENHA="$POSTGRES_PASSWORD"
    export DB_URL="jdbc:postgresql://127.0.0.1:5432/${POSTGRES_DB:-odonto}"
    log "Iniciando PostgreSQL 18 embutido..."
    docker-entrypoint.sh postgres -c listen_addresses=127.0.0.1 -c timezone=UTC &
    PG_PID=$!
    for _ in $(seq 1 60); do
        # Na primeira inicialização, a imagem oficial sobe um servidor temporário só no socket Unix;
        # por isso a espera é via TCP, que só responde quando o servidor definitivo está no ar.
        pg_isready -q -h 127.0.0.1 -U "$DB_USUARIO" -d "${POSTGRES_DB:-odonto}" 2>/dev/null && break
        sleep 1
    done
    pg_isready -q -h 127.0.0.1 -U "$DB_USUARIO" -d "${POSTGRES_DB:-odonto}" || { log "PostgreSQL não respondeu."; exit 1; }
    log "PostgreSQL pronto."
else
    log "Usando banco externo: ${DB_URL%%\?*}"
fi

log "Iniciando aplicação..."
gosu odonto java ${JAVA_OPTS:--XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError} -jar /app/app.jar &
APP_PID=$!

# Termina quando qualquer processo cair, para o Docker reiniciar o contêiner (restart: unless-stopped).
set +e
wait -n
STATUS=$?
log "Um dos processos terminou (código $STATUS)."
encerrar "$STATUS"

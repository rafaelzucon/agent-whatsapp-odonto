#!/usr/bin/env bash
# Operação do Agente WhatsApp Odonto (Docker). Uso: ./odonto.sh <comando>  |  ./odonto.sh ajuda
set -euo pipefail

cd "$(dirname "$0")"

CONTEINER="agent-whatsapp-odonto"
ARQUIVO_ENV=".env"
PASTA_BACKUP="backups"

vermelho() { printf '\033[31m%s\033[0m\n' "$*"; }
verde() { printf '\033[32m%s\033[0m\n' "$*"; }
amarelo() { printf '\033[33m%s\033[0m\n' "$*"; }
falhar() { vermelho "Erro: $*" >&2; exit 1; }

compose() {
    if docker compose version >/dev/null 2>&1; then
        docker compose "$@"
    elif command -v docker-compose >/dev/null 2>&1; then
        docker-compose "$@"
    else
        falhar "Docker Compose não encontrado. Instale o Docker com o plugin compose."
    fi
}

# Lê uma variável do .env sem executar o arquivo (valores podem ter espaços).
valor_env() {
    [[ -f "$ARQUIVO_ENV" ]] || return 0
    grep -E "^$1=" "$ARQUIVO_ENV" | tail -1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//'
}

definir_env() {
    local chave="$1" valor="$2"
    if grep -qE "^$chave=" "$ARQUIVO_ENV"; then
        local escapado
        escapado=$(printf '%s' "$valor" | sed -e 's/[\/&|]/\\&/g')
        sed -i.bak -E "s|^$chave=.*|$chave=$escapado|" "$ARQUIVO_ENV" && rm -f "$ARQUIVO_ENV.bak"
    else
        printf '%s=%s\n' "$chave" "$valor" >>"$ARQUIVO_ENV"
    fi
}

aleatorio_base64() { openssl rand -base64 32; }
aleatorio_hex() { openssl rand -hex "${1:-24}"; }

exigir_docker() {
    command -v docker >/dev/null 2>&1 || falhar "Docker não encontrado. Veja https://docs.docker.com/engine/install/"
    docker info >/dev/null 2>&1 || falhar "O Docker não está rodando ou o usuário não tem permissão (tente sudo)."
}

exigir_env() {
    [[ -f "$ARQUIVO_ENV" ]] || falhar "Arquivo .env não encontrado. Rode primeiro: ./odonto.sh configurar"
}

porta() { local p; p=$(valor_env PORTA_HOST); echo "${p:-8080}"; }

cmd_configurar() {
    command -v openssl >/dev/null 2>&1 || falhar "openssl é necessário para gerar as chaves."
    if [[ ! -f "$ARQUIVO_ENV" ]]; then
        cp .env.example "$ARQUIVO_ENV"
        chmod 600 "$ARQUIVO_ENV"
        verde "Criado $ARQUIVO_ENV a partir de .env.example."
    fi
    local gerou=0
    for chave in CRIPTO_CHAVE_DADOS CRIPTO_CHAVE_HASH; do
        if [[ -z "$(valor_env "$chave")" ]]; then definir_env "$chave" "$(aleatorio_base64)"; gerou=1; fi
    done
    [[ -z "$(valor_env DB_SENHA)" ]] && definir_env DB_SENHA "$(aleatorio_hex 18)" && gerou=1
    [[ -z "$(valor_env ADMIN_TOKEN)" ]] && definir_env ADMIN_TOKEN "$(aleatorio_hex 24)" && gerou=1
    [[ -z "$(valor_env WHATSAPP_TOKEN_VERIFICACAO)" ]] && definir_env WHATSAPP_TOKEN_VERIFICACAO "$(aleatorio_hex 16)" && gerou=1
    if [[ $gerou -eq 1 ]]; then
        verde "Chaves e senhas geradas no $ARQUIVO_ENV."
        amarelo "IMPORTANTE: guarde uma cópia segura de CRIPTO_CHAVE_DADOS e CRIPTO_CHAVE_HASH."
        amarelo "Sem elas, os dados criptografados (RG, nomes, telefones) não podem ser recuperados."
    fi
    echo "Agora edite $ARQUIVO_ENV e preencha os dados do consultório, do WhatsApp e da Anthropic."
    echo "Depois rode: ./odonto.sh verificar && ./odonto.sh iniciar"
}

cmd_verificar() {
    exigir_env
    local faltando=() avisos=()
    for chave in WHATSAPP_PHONE_NUMBER_ID WHATSAPP_TOKEN_ACESSO WHATSAPP_TOKEN_VERIFICACAO WHATSAPP_SEGREDO_APP \
        ANTHROPIC_API_KEY CRIPTO_CHAVE_DADOS CRIPTO_CHAVE_HASH DB_SENHA; do
        [[ -z "$(valor_env "$chave")" ]] && faltando+=("$chave")
    done
    [[ -z "$(valor_env WHATSAPP_NUMERO_CONSULTORIO)" ]] && avisos+=("WHATSAPP_NUMERO_CONSULTORIO vazio: o consultório não receberá avisos pelo WhatsApp.")
    [[ -z "$(valor_env OPENAI_API_KEY)" ]] && avisos+=("OPENAI_API_KEY vazio: áudios não serão transcritos (o agente pede texto).")
    [[ -z "$(valor_env ADMIN_TOKEN)" ]] && avisos+=("ADMIN_TOKEN vazio: a API administrativa ficará desligada.")
    if [[ "$(valor_env CRIPTO_CHAVE_DADOS)" == "$(valor_env CRIPTO_CHAVE_HASH)" && -n "$(valor_env CRIPTO_CHAVE_DADOS)" ]]; then
        faltando+=("CRIPTO_CHAVE_DADOS e CRIPTO_CHAVE_HASH devem ser diferentes")
    fi
    for aviso in "${avisos[@]}"; do amarelo "Aviso: $aviso"; done
    if [[ ${#faltando[@]} -gt 0 ]]; then
        vermelho "Configuração incompleta. Preencha no $ARQUIVO_ENV:"
        printf '  - %s\n' "${faltando[@]}"
        exit 1
    fi
    verde "Configuração OK."
}

cmd_construir() { exigir_docker; compose build "$@"; }

aguardar_saude() {
    local url="http://localhost:$(porta)/actuator/health"
    printf 'Aguardando a aplicação ficar saudável'
    for _ in $(seq 1 60); do
        if curl -fsS "$url" 2>/dev/null | grep -q '"UP"'; then
            echo; verde "Aplicação no ar em http://localhost:$(porta)"; return 0
        fi
        printf '.'; sleep 3
    done
    echo; vermelho "A aplicação não respondeu a tempo. Veja: ./odonto.sh logs"; return 1
}

cmd_iniciar() {
    exigir_docker; exigir_env
    cmd_verificar
    compose up -d --build
    aguardar_saude
}

cmd_parar() { exigir_docker; compose stop; verde "Parado."; }

cmd_reiniciar() { exigir_docker; exigir_env; compose restart; aguardar_saude; }

cmd_remover() {
    exigir_docker
    amarelo "Isto remove o contêiner. Os dados do banco (volume) são mantidos."
    compose down
}

cmd_status() {
    exigir_docker
    compose ps
    echo
    if curl -fsS "http://localhost:$(porta)/actuator/health" 2>/dev/null; then echo; else vermelho "Aplicação não responde."; fi
}

cmd_logs() { exigir_docker; compose logs --tail="${1:-200}" -f; }

cmd_backup() {
    exigir_docker
    mkdir -p "$PASTA_BACKUP"
    local arquivo="$PASTA_BACKUP/odonto-$(date +%Y%m%d-%H%M%S).dump"
    docker exec "$CONTEINER" pg_dump -U odonto -d odonto -Fc >"$arquivo"
    chmod 600 "$arquivo"
    verde "Backup salvo em $arquivo ($(du -h "$arquivo" | cut -f1))."
    amarelo "O backup contém dados de saúde (LGPD): guarde-o criptografado e fora do servidor."
}

cmd_restaurar() {
    exigir_docker
    local arquivo="${1:-}"
    [[ -f "$arquivo" ]] || falhar "Informe o arquivo: ./odonto.sh restaurar backups/odonto-AAAAMMDD-HHMMSS.dump"
    amarelo "ATENÇÃO: o banco atual será substituído pelo conteúdo de $arquivo."
    read -r -p "Digite RESTAURAR para confirmar: " confirmacao
    [[ "$confirmacao" == "RESTAURAR" ]] || falhar "Cancelado."
    cmd_backup
    docker exec -i "$CONTEINER" pg_restore -U odonto -d odonto --clean --if-exists --no-owner <"$arquivo"
    compose restart
    verde "Banco restaurado."
    aguardar_saude
}

cmd_atualizar() {
    exigir_docker; exigir_env
    if [[ -d .git ]]; then git pull --ff-only; fi
    if docker ps --format '{{.Names}}' | grep -qx "$CONTEINER"; then cmd_backup; fi
    compose up -d --build
    aguardar_saude
}

cmd_psql() { exigir_docker; docker exec -it "$CONTEINER" psql -U odonto -d odonto; }

cmd_testar() { ./mvnw -B verify; }

cmd_ajuda() {
    cat <<'TEXTO'
Agente WhatsApp Odonto - operação

Uso: ./odonto.sh <comando>

  configurar        cria o .env e gera chaves de criptografia, senha do banco e tokens
  verificar         confere se as variáveis obrigatórias do .env estão preenchidas
  construir         constrói a imagem Docker
  iniciar           constrói (se preciso), sobe o contêiner e espera ficar saudável
  parar             para o contêiner (dados preservados)
  reiniciar         reinicia o contêiner
  remover           remove o contêiner (o volume com o banco é mantido)
  status            mostra o estado do contêiner e a saúde da aplicação
  logs [linhas]     acompanha os logs (padrão: últimas 200 linhas)
  backup            gera um dump do PostgreSQL em ./backups
  restaurar <arq>   restaura um dump (faz backup do estado atual antes)
  atualizar         git pull + backup + reconstrução + reinício
  psql              abre o console do PostgreSQL
  testar            roda os testes automatizados (./mvnw verify)
  ajuda             mostra esta ajuda
TEXTO
}

comando="${1:-ajuda}"
shift || true
case "$comando" in
    configurar) cmd_configurar ;;
    verificar) cmd_verificar ;;
    construir) cmd_construir "$@" ;;
    iniciar) cmd_iniciar ;;
    parar) cmd_parar ;;
    reiniciar) cmd_reiniciar ;;
    remover) cmd_remover ;;
    status) cmd_status ;;
    logs) cmd_logs "$@" ;;
    backup) cmd_backup ;;
    restaurar) cmd_restaurar "$@" ;;
    atualizar) cmd_atualizar ;;
    psql) cmd_psql ;;
    testar) cmd_testar ;;
    ajuda | -h | --help) cmd_ajuda ;;
    *) vermelho "Comando desconhecido: $comando"; cmd_ajuda; exit 1 ;;
esac

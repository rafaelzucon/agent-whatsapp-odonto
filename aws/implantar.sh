#!/usr/bin/env bash
# Implantação do Agente WhatsApp Odonto no AWS Lambda (SAM). Uso: ./aws/implantar.sh <comando> | ajuda
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"

TEMPLATE="aws/template.yaml"
PARAMETROS="aws/parametros.env"
SEGREDOS="aws/segredos.json"
PACOTE="target/agent-whatsapp-odonto-lambda.zip"
FUNCOES="webhook conversa manutencao admin migracao"

vermelho() { printf '\033[31m%s\033[0m\n' "$*"; }
verde() { printf '\033[32m%s\033[0m\n' "$*"; }
amarelo() { printf '\033[33m%s\033[0m\n' "$*"; }
falhar() { vermelho "Erro: $*" >&2; exit 1; }

# Lê uma chave do parametros.env sem executar o arquivo (valores podem ter espaços e acentos).
parametro() {
    [[ -f "$PARAMETROS" ]] || return 0
    grep -E "^$1=" "$PARAMETROS" | tail -1 | cut -d= -f2-
}

pilha() { local p; p=$(parametro PILHA); echo "${p:-agente-odonto}"; }
regiao() { local r; r=$(parametro REGIAO); echo "${r:-sa-east-1}"; }
nome_segredo() { local n; n=$(parametro NOME_SEGREDO); echo "${n:-$(pilha)/segredos}"; }
aws_() { aws --region "$(regiao)" "$@"; }

exigir() { command -v "$1" >/dev/null 2>&1 || falhar "$1 não encontrado. $2"; }

exigir_ferramentas() {
    exigir aws "Instale a AWS CLI v2: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
    exigir sam "Instale o AWS SAM CLI: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html"
    exigir python3 "O Python 3 é usado para ler e comparar o JSON dos segredos."
    aws_ sts get-caller-identity >/dev/null 2>&1 || falhar "Credenciais da AWS ausentes ou expiradas (aws configure / aws sso login)."
}

exigir_configuracao() {
    [[ -f "$PARAMETROS" && -f "$SEGREDOS" ]] || falhar "Configuração não encontrada. Rode primeiro: ./aws/implantar.sh configurar"
    if grep -q "PREENCHA" "$PARAMETROS" "$SEGREDOS"; then
        grep -n "PREENCHA" "$PARAMETROS" "$SEGREDOS" >&2
        falhar "Preencha os valores marcados com PREENCHA acima."
    fi
    python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert isinstance(d, dict)' "$SEGREDOS" 2>/dev/null \
        || falhar "$SEGREDOS não é um objeto JSON válido."
}

cmd_configurar() {
    exigir openssl "Necessário para gerar chaves aleatórias."
    if [[ ! -f "$PARAMETROS" ]]; then
        cp aws/parametros.exemplo.env "$PARAMETROS"
        verde "Criado $PARAMETROS."
    else
        amarelo "$PARAMETROS já existe; mantido."
    fi
    if [[ ! -f "$SEGREDOS" ]]; then
        # Senha do RDS só com caracteres aceitos (sem / @ " e espaço).
        python3 - "$SEGREDOS" "$(openssl rand -base64 32)" "$(openssl rand -base64 32)" \
            "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" "$(openssl rand -hex 16)" <<'PY'
import json, sys
arquivo, dados, hash_, senha, admin, verificacao = sys.argv[1:]
d = json.load(open("aws/segredos.exemplo.json"))
d.update({"CRIPTO_CHAVE_DADOS": dados, "CRIPTO_CHAVE_HASH": hash_, "DB_SENHA": senha,
          "ADMIN_TOKEN": admin, "WHATSAPP_TOKEN_VERIFICACAO": verificacao})
json.dump(d, open(arquivo, "w"), indent=2)
PY
        chmod 600 "$SEGREDOS"
        verde "Criado $SEGREDOS com chaves de criptografia, senha do banco e tokens gerados."
        amarelo "Guarde uma cópia segura das chaves CRIPTO_*: sem elas os dados cifrados não podem ser lidos."
    else
        amarelo "$SEGREDOS já existe; mantido."
    fi
    echo
    echo "Agora edite os valores marcados com PREENCHA em $PARAMETROS e $SEGREDOS e rode:"
    echo "  ./aws/implantar.sh implantar"
}

cmd_verificar() {
    exigir_configuracao
    exigir_ferramentas
    sam validate --lint -t "$TEMPLATE" --region "$(regiao)"
    verde "Configuração e template válidos. Conta: $(aws_ sts get-caller-identity --query Account --output text), região: $(regiao)."
}

cmd_construir() {
    ./mvnw -B -Plambda package "$@"
    local tamanho
    tamanho=$(python3 -c 'import sys,zipfile; print(sum(i.file_size for i in zipfile.ZipFile(sys.argv[1]).infolist()))' "$PACOTE")
    (( tamanho < 250 * 1024 * 1024 )) || falhar "Pacote com $((tamanho / 1048576)) MB descompactado (limite do Lambda: 250 MB)."
    verde "Pacote $PACOTE pronto ($((tamanho / 1048576)) MB descompactado)."
}

# Cria ou atualiza o segredo e imprime o ARN e o VersionId atual.
publicar_segredo() {
    local nome arn atual
    nome=$(nome_segredo)
    if arn=$(aws_ secretsmanager describe-secret --secret-id "$nome" --query ARN --output text 2>/dev/null); then
        atual=$(aws_ secretsmanager get-secret-value --secret-id "$nome" --query SecretString --output text)
        if ! python3 -c 'import json,sys; sys.exit(0 if json.loads(sys.argv[1]) == json.load(open(sys.argv[2])) else 1)' \
            "$atual" "$SEGREDOS"; then
            ajustar_senha_banco "$atual" >&2
            aws_ secretsmanager put-secret-value --secret-id "$nome" --secret-string "file://$SEGREDOS" >/dev/null
            amarelo "Segredo $nome atualizado." >&2
        fi
    else
        arn=$(aws_ secretsmanager create-secret --name "$nome" \
            --description "Segredos do Agente WhatsApp Odonto (pilha $(pilha))" \
            --secret-string "file://$SEGREDOS" --query ARN --output text)
        verde "Segredo $nome criado." >&2
    fi
    echo "$arn $(aws_ secretsmanager get-secret-value --secret-id "$nome" --query VersionId --output text)"
}

# O RDS não acompanha mudanças no segredo: se DB_SENHA mudou, troca a senha mestre antes.
ajustar_senha_banco() {
    local senha_antiga senha_nova instancia
    senha_antiga=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1]).get("DB_SENHA",""))' "$1")
    senha_nova=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("DB_SENHA",""))' "$SEGREDOS")
    [[ "$senha_antiga" == "$senha_nova" ]] && return 0
    instancia=$(aws_ cloudformation describe-stack-resource --stack-name "$(pilha)" --logical-resource-id Banco \
        --query StackResourceDetail.PhysicalResourceId --output text 2>/dev/null) || return 0
    amarelo "DB_SENHA mudou: alterando a senha mestre do RDS $instancia..."
    aws_ rds modify-db-instance --db-instance-identifier "$instancia" --master-user-password "$senha_nova" \
        --apply-immediately >/dev/null
    aws_ rds wait db-instance-available --db-instance-identifier "$instancia"
}

parametros_template() {
    local linha chave valor
    while IFS= read -r linha || [[ -n "$linha" ]]; do
        [[ "$linha" =~ ^[A-Z][A-Za-z0-9]*= ]] || continue
        chave="${linha%%=*}"
        valor="${linha#*=}"
        case "$chave" in PILHA | REGIAO | NOME_SEGREDO) continue ;; esac
        valor="${valor//\\/\\\\}"
        printf '%s="%s"\n' "$chave" "${valor//\"/\\\"}"
    done <"$PARAMETROS"
}

cmd_implantar() {
    exigir_configuracao
    exigir_ferramentas
    [[ -f "$PACOTE" && "${1:-}" != "--construir" ]] || cmd_construir
    local segredo arn versao_segredo versao
    segredo=$(publicar_segredo)
    arn=${segredo% *}
    versao_segredo=${segredo#* }
    # Hash de tudo que entra no snapshot do SnapStart: código, template, parâmetros e versão do segredo.
    versao=$( { sha256sum "$PACOTE" "$TEMPLATE" "$PARAMETROS"; echo "$versao_segredo"; } | sha256sum | cut -c1-10)

    local overrides=("SegredoArn=$arn" "VersaoImplantacao=$versao")
    while IFS= read -r p; do overrides+=("$p"); done < <(parametros_template)

    verde "Implantando a pilha $(pilha) em $(regiao) (versão $versao)..."
    sam deploy \
        --template-file "$TEMPLATE" \
        --stack-name "$(pilha)" \
        --region "$(regiao)" \
        --resolve-s3 \
        --s3-prefix "$(pilha)" \
        --capabilities CAPABILITY_IAM \
        --no-confirm-changeset \
        --no-fail-on-empty-changeset \
        --tags "aplicacao=agent-whatsapp-odonto" \
        --parameter-overrides "${overrides[@]}"
    echo
    cmd_saidas
    echo
    echo "Cadastre no painel da Meta (WhatsApp > Configuração > Webhook):"
    echo "  URL de callback: a UrlWebhook acima"
    echo "  Token de verificação: o WHATSAPP_TOKEN_VERIFICACAO de $SEGREDOS"
    echo "  Campo assinado: messages"
}

cmd_saidas() {
    aws_ cloudformation describe-stacks --stack-name "$(pilha)" \
        --query 'Stacks[0].Outputs[].[OutputKey,OutputValue]' --output table
}

cmd_logs() {
    local funcao="${1:-conversa}"
    [[ " $FUNCOES " == *" $funcao "* ]] || falhar "Função desconhecida: $funcao. Use: $FUNCOES"
    aws_ logs tail "/aws/lambda/$(pilha)-$funcao" --follow --since "${2:-1h}"
}

cmd_falhas() {
    local url
    url=$(aws_ cloudformation describe-stacks --stack-name "$(pilha)" \
        --query "Stacks[0].Outputs[?OutputKey=='FilaMensagensFalhas'].OutputValue" --output text)
    aws_ sqs get-queue-attributes --queue-url "$url" --attribute-names ApproximateNumberOfMessages \
        --query 'Attributes.ApproximateNumberOfMessages' --output text | xargs printf 'Mensagens na fila de falhas: %s\n'
}

cmd_migrar() {
    local saida=/tmp/odonto-migracao-$$.json
    aws_ lambda invoke --function-name "$(pilha)-migracao:atual" --cli-read-timeout 310 "$saida" >/dev/null
    cat "$saida"; echo; rm -f "$saida"
}

cmd_remover() {
    amarelo "Isto remove todas as funções, a fila, a rede e o banco da pilha $(pilha) (fica um snapshot final do RDS)."
    read -r -p "Digite o nome da pilha para confirmar: " confirmacao
    [[ "$confirmacao" == "$(pilha)" ]] || falhar "Confirmação incorreta; nada foi removido."
    local instancia
    if instancia=$(aws_ cloudformation describe-stack-resource --stack-name "$(pilha)" --logical-resource-id Banco \
        --query StackResourceDetail.PhysicalResourceId --output text 2>/dev/null); then
        aws_ rds modify-db-instance --db-instance-identifier "$instancia" --no-deletion-protection \
            --apply-immediately >/dev/null
    fi
    sam delete --stack-name "$(pilha)" --region "$(regiao)" --no-prompts
    amarelo "O segredo $(nome_segredo) foi mantido. Para apagá-lo: aws secretsmanager delete-secret --secret-id $(nome_segredo)"
}

cmd_ajuda() {
    cat <<'AJUDA'
Uso: ./aws/implantar.sh <comando>

  configurar           cria aws/parametros.env e aws/segredos.json (chaves e senhas geradas)
  verificar            confere configuração, ferramentas, credenciais e o template
  construir            testa e gera target/agent-whatsapp-odonto-lambda.zip (./mvnw -Plambda package)
  implantar            publica o segredo e implanta a pilha (gera o pacote se não existir)
  implantar --construir   reconstrói o pacote antes de implantar
  saidas               mostra a URL do webhook, a URL da API admin e as filas
  logs [funcao] [desde]   acompanha os logs (webhook|conversa|manutencao|admin|migracao; padrão conversa 1h)
  falhas               quantidade de mensagens na fila de falhas (DLQ)
  migrar               executa as migrações do banco manualmente
  remover              remove a pilha (o RDS deixa um snapshot final; o segredo é mantido)

Pré-requisitos: AWS CLI v2, AWS SAM CLI, Java 21, Python 3 e openssl.
AJUDA
}

comando="${1:-ajuda}"
shift || true
case "$comando" in
    configurar) cmd_configurar ;;
    verificar) cmd_verificar ;;
    construir) cmd_construir "$@" ;;
    implantar) cmd_implantar "$@" ;;
    saidas) cmd_saidas ;;
    logs) cmd_logs "$@" ;;
    falhas) cmd_falhas ;;
    migrar) cmd_migrar ;;
    remover) cmd_remover ;;
    ajuda | -h | --help) cmd_ajuda ;;
    *) vermelho "Comando desconhecido: $comando"; cmd_ajuda; exit 1 ;;
esac

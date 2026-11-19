#!/usr/bin/env bash
# Implantação do Agente WhatsApp Odonto no AWS Lambda (SAM). Uso: ./aws/implantar.sh <comando> | ajuda
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"

TEMPLATE="aws/template.yaml"
PARAMETROS="aws/parametros.env"
SEGREDOS="aws/segredos.json"
PACOTE="target/agent-whatsapp-odonto-lambda.zip"
FUNCOES="webhook conversa manutencao admin"

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
caminho_segredos() { echo "/$(pilha)/segredos"; }
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
        python3 - "$SEGREDOS" "$(openssl rand -base64 32)" "$(openssl rand -base64 32)" \
            "$(openssl rand -hex 24)" "$(openssl rand -hex 16)" <<'PY'
import json, sys
arquivo, dados, hash_, admin, verificacao = sys.argv[1:]
d = json.load(open("aws/segredos.exemplo.json"))
d.update({"CRIPTO_CHAVE_DADOS": dados, "CRIPTO_CHAVE_HASH": hash_,
          "ADMIN_TOKEN": admin, "WHATSAPP_TOKEN_VERIFICACAO": verificacao})
json.dump(d, open(arquivo, "w"), indent=2)
PY
        chmod 600 "$SEGREDOS"
        verde "Criado $SEGREDOS com chaves de criptografia e tokens gerados."
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

# Sincroniza aws/segredos.json com o Parameter Store (/<pilha>/segredos/<CHAVE>, SecureString com a chave
# gerenciada aws/ssm, sem custo): grava as chaves novas ou alteradas e apaga as removidas ou vazias (o Parameter
# Store não aceita valor vazio). Os valores passam por arquivos temporários, nunca pela linha de comando.
publicar_segredos() {
    local caminho atual temporario f chave
    caminho=$(caminho_segredos)
    atual=$(aws_ ssm get-parameters-by-path --path "$caminho" --with-decryption \
        --query 'Parameters[].[Name,Value]' --output json)
    temporario=$(mktemp -d)
    chmod 700 "$temporario"
    python3 - "$SEGREDOS" "$caminho" "$temporario" "$atual" <<'PY'
import json, os, sys
arquivo, caminho, pasta, atual = sys.argv[1:]
desejado = {k: str(v) for k, v in json.load(open(arquivo)).items() if v is not None and str(v) != ""}
existente = {nome.rsplit("/", 1)[1]: valor for nome, valor in (json.loads(atual) or [])}
for chave, valor in desejado.items():
    if existente.get(chave) != valor:
        entrada = {"Name": f"{caminho}/{chave}", "Value": valor, "Type": "SecureString", "Overwrite": True,
                   "Tier": "Standard"}
        with open(os.open(os.path.join(pasta, f"gravar-{chave}.json"), os.O_WRONLY | os.O_CREAT, 0o600), "w") as f:
            json.dump(entrada, f)
for chave in existente.keys() - desejado.keys():
    open(os.path.join(pasta, f"apagar-{chave}"), "w").close()
PY
    for f in "$temporario"/gravar-*.json; do
        [[ -e "$f" ]] || continue
        chave=$(basename "$f" .json)
        chave=${chave#gravar-}
        aws_ ssm put-parameter --cli-input-json "file://$f" >/dev/null
        amarelo "Segredo $caminho/$chave gravado."
    done
    for f in "$temporario"/apagar-*; do
        [[ -e "$f" ]] || continue
        chave=${f##*/apagar-}
        aws_ ssm delete-parameter --name "$caminho/$chave" >/dev/null
        amarelo "Segredo $caminho/$chave removido."
    done
    rm -rf "$temporario"
}

parametros_template() {
    local linha chave valor
    while IFS= read -r linha || [[ -n "$linha" ]]; do
        [[ "$linha" =~ ^[A-Z][A-Za-z0-9]*= ]] || continue
        chave="${linha%%=*}"
        valor="${linha#*=}"
        case "$chave" in PILHA | REGIAO) continue ;; esac
        valor="${valor//\\/\\\\}"
        printf '%s="%s"\n' "$chave" "${valor//\"/\\\"}"
    done <"$PARAMETROS"
}

cmd_implantar() {
    exigir_configuracao
    exigir_ferramentas
    [[ -f "$PACOTE" && "${1:-}" != "--construir" ]] || cmd_construir
    publicar_segredos
    local versao
    # Hash de tudo que entra no snapshot do SnapStart: código, template, parâmetros e segredos.
    versao=$(sha256sum "$PACOTE" "$TEMPLATE" "$PARAMETROS" "$SEGREDOS" | sha256sum | cut -c1-10)

    local overrides=("VersaoImplantacao=$versao")
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

cmd_backup() {
    local nome
    nome="$(pilha)-dados-$(date +%Y%m%d-%H%M%S)"
    aws_ dynamodb create-backup --table-name "$(pilha)-dados" --backup-name "$nome" \
        --query 'BackupDetails.[BackupName,BackupStatus]' --output text
}

cmd_remover() {
    amarelo "Isto remove as funções, as filas, os agendamentos e os logs da pilha $(pilha)."
    amarelo "A tabela DynamoDB (dados dos pacientes) e os segredos do Parameter Store são MANTIDOS."
    read -r -p "Digite o nome da pilha para confirmar: " confirmacao
    [[ "$confirmacao" == "$(pilha)" ]] || falhar "Confirmação incorreta; nada foi removido."
    sam delete --stack-name "$(pilha)" --region "$(regiao)" --no-prompts
    echo
    echo "Para apagar também os dados (irreversível; rode antes ./aws/implantar.sh backup, se precisar):"
    echo "  aws --region $(regiao) dynamodb update-table --table-name $(pilha)-dados --no-deletion-protection-enabled"
    echo "  aws --region $(regiao) dynamodb delete-table --table-name $(pilha)-dados"
    echo "Para apagar os segredos:"
    echo "  aws --region $(regiao) ssm get-parameters-by-path --path $(caminho_segredos) --query 'Parameters[].Name' --output text \\"
    echo "    | xargs -n 10 aws --region $(regiao) ssm delete-parameters --names"
}

cmd_ajuda() {
    cat <<'AJUDA'
Uso: ./aws/implantar.sh <comando>

  configurar           cria aws/parametros.env e aws/segredos.json (chaves e tokens gerados)
  verificar            confere configuração, ferramentas, credenciais e o template
  construir            testa e gera target/agent-whatsapp-odonto-lambda.zip (./mvnw -Plambda package)
  implantar            publica os segredos no Parameter Store e implanta a pilha (gera o pacote se não existir)
  implantar --construir   reconstrói o pacote antes de implantar
  saidas               mostra a URL do webhook, a URL da API admin, a tabela e as filas
  logs [funcao] [desde]   acompanha os logs (webhook|conversa|manutencao|admin; padrão conversa 1h)
  falhas               quantidade de mensagens na fila de falhas (DLQ)
  backup               cria um backup sob demanda da tabela DynamoDB
  remover              remove a pilha (a tabela DynamoDB e os segredos são mantidos)

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
    backup) cmd_backup ;;
    remover) cmd_remover ;;
    ajuda | -h | --help) cmd_ajuda ;;
    *) vermelho "Comando desconhecido: $comando"; cmd_ajuda; exit 1 ;;
esac

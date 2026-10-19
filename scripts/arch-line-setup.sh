#!/usr/bin/env bash
# Arch line — setup do Hub e dos secrets dos 4 repositórios.
#
#   scripts/arch-line-setup.sh tokens              # 1. gera os tokens (uma vez) e mostra a linha do Hub
#   scripts/arch-line-setup.sh secrets <HUB_URL>   # 2. grava ARCHHUB_URL/ARCHHUB_TOKEN nos 4 repos (gh)
#   scripts/arch-line-setup.sh check <HUB_URL>     # 3. Hub no ar? tokens aceitos?
#   scripts/arch-line-setup.sh publish <HUB_URL>   # 4. dispara o workflow "Arch line" nos 4 repos e lista os scores
#   scripts/arch-line-setup.sh runtime <HUB_URL>   #    variáveis para o registro em runtime (PVD, Vercel, archmap serve)
#
# Os tokens ficam em .arch-line/tokens.env (fora do git, chmod 600). Rodar de novo reaproveita
# os mesmos tokens; apague o arquivo para gerar novos (e atualize o Hub e os secrets).
# Requer: bash, openssl, curl e — para secrets/publish — gh autenticado (gh auth login).
# Contrato: docs/ARCH-LINE.md.
set -euo pipefail

OWNER="Kaua-KGzin"
# repo:id-no-hub:arquivo-do-workflow
LINE=(
  "System-Arch:arch-hub:arch-line.yml"
  "System-PVD:system-pvd:arch-line.yml"
  "SIMPLE-ArCh:simple-arch:arch-line.yml"
  "ArchMAP:archmap:arch-line-report.yml"
)

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT/.arch-line"
TOKENS_FILE="$STATE_DIR/tokens.env"

die() { echo "erro: $*" >&2; exit 1; }
info() { echo "→ $*"; }

need() { command -v "$1" >/dev/null 2>&1 || die "'$1' não encontrado. $2"; }

hub_url_arg() {
  local url="${1:-}"
  [[ -n "$url" ]] || die "informe a URL pública do Hub, ex.: https://arch-hub.onrender.com"
  [[ "$url" =~ ^https?:// ]] || die "a URL precisa começar com http:// ou https://"
  [[ "$url" =~ ^https?://(localhost|127\.) ]] && die "localhost não serve: o CI roda nos servidores do GitHub (veja deploy/README.md)"
  echo "${url%/}"
}

token_for() { # id -> token (de tokens.env)
  local id="$1" key
  key="TOKEN_$(echo "$id" | tr 'a-z-' 'A-Z_')"
  # shellcheck disable=SC1090
  (source "$TOKENS_FILE" && printf '%s' "${!key:-}")
}

require_tokens() { [[ -f "$TOKENS_FILE" ]] || die "rode primeiro: $0 tokens"; }

cmd_tokens() {
  need openssl "Instale o OpenSSL."
  mkdir -p "$STATE_DIR"
  chmod 700 "$STATE_DIR"
  if [[ -f "$TOKENS_FILE" ]]; then
    info "reaproveitando os tokens de $TOKENS_FILE"
  else
    umask 077
    {
      echo "# Arch line — tokens por sistema. NÃO commitar."
      for entry in "${LINE[@]}"; do
        IFS=: read -r _ id _ <<<"$entry"
        echo "TOKEN_$(echo "$id" | tr 'a-z-' 'A-Z_')=$(openssl rand -hex 24)"
      done
    } >"$TOKENS_FILE"
    info "tokens gerados em $TOKENS_FILE"
  fi
  local line="" id
  for entry in "${LINE[@]}"; do
    IFS=: read -r _ id _ <<<"$entry"
    line+="${line:+,}$id=$(token_for "$id")"
  done
  echo
  echo "Configure no Hub (Render: Environment; self-host: deploy/self-host/.env):"
  echo
  echo "ARCHHUB_SYSTEM_TOKENS=$line"
  echo
}

cmd_secrets() {
  local hub; hub="$(hub_url_arg "${1:-}")"
  need gh "Instale o GitHub CLI: https://cli.github.com"
  gh auth status >/dev/null 2>&1 || die "gh não autenticado: rode 'gh auth login'"
  require_tokens
  local repo id token
  for entry in "${LINE[@]}"; do
    IFS=: read -r repo id _ <<<"$entry"
    token="$(token_for "$id")"
    [[ -n "$token" ]] || die "sem token para $id em $TOKENS_FILE"
    # Valores via stdin: nunca aparecem na linha de comando (ps, histórico).
    printf '%s' "$hub" | gh secret set ARCHHUB_URL --repo "$OWNER/$repo" >/dev/null
    printf '%s' "$token" | gh secret set ARCHHUB_TOKEN --repo "$OWNER/$repo" >/dev/null
    info "$OWNER/$repo: ARCHHUB_URL e ARCHHUB_TOKEN ($id) gravados"
  done
}

cmd_check() {
  local hub; hub="$(hub_url_arg "${1:-}")"
  need curl "Instale o curl."
  require_tokens
  local health
  health="$(curl -fsS --max-time 30 "$hub/actuator/health" 2>/dev/null)" \
    || die "Hub não respondeu em $hub/actuator/health (no Render free a primeira chamada acorda o serviço: tente de novo em ~1 min)"
  info "Hub no ar: $health"
  local id code ok=0
  for entry in "${LINE[@]}"; do
    IFS=: read -r _ id _ <<<"$entry"
    # Heartbeat com o token do próprio sistema: 200 (registrado) ou 404 (ainda não registrado)
    # provam que o token foi aceito; 401/403 = token ausente/errado no Hub.
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 -X POST \
      -H "X-Hub-Token: $(token_for "$id")" "$hub/api/v1/systems/$id/heartbeat")"
    case "$code" in
      200|404) info "token de $id aceito (HTTP $code)" ;;
      *) echo "✗ token de $id recusado (HTTP $code) — confira ARCHHUB_SYSTEM_TOKENS no Hub" >&2; ok=1 ;;
    esac
  done
  return $ok
}

cmd_publish() {
  local hub; hub="$(hub_url_arg "${1:-}")"
  need gh "Instale o GitHub CLI: https://cli.github.com"
  need curl "Instale o curl."
  local repo file
  for entry in "${LINE[@]}"; do
    IFS=: read -r repo _ file <<<"$entry"
    gh workflow run "$file" --repo "$OWNER/$repo" --ref main >/dev/null
    info "$OWNER/$repo: workflow $file disparado"
  done
  local wait="${ARCH_LINE_WAIT:-150}"
  info "aguardando os jobs (${wait}s; ARCH_LINE_WAIT muda isso)…"
  sleep "$wait"
  curl -fsS --max-time 30 "$hub/api/v1/architecture" | python3 -c '
import json, sys
rows = json.load(sys.stdin)
if not rows:
    sys.exit("nenhum score ainda — veja a aba Actions de cada repo (resumo do job \"Arch line\")")
for r in rows:
    a = (r.get("architecture") or {}).get("latest") or {}
    print("  %-12s %3s/100 %-2s (%s %s)" % (r["systemId"], a.get("score", "?"), a.get("grade") or "",
                                          a.get("ref") or "-", str(a.get("commit") or "")[:7]))
'
}

cmd_runtime() {
  local hub; hub="$(hub_url_arg "${1:-}")"
  require_tokens
  cat <<EOF
Registro em runtime (opcional — o CI não precisa disto):

System-PVD (.env na raiz, usado pelo docker compose):
  ARCHHUB_URL=$hub
  ARCHHUB_TOKEN=$(token_for system-pvd)
  # ARCHHUB_PUBLIC_URL=<endereço público do PDV, se o Hub não estiver na mesma máquina>

SIMPLE-ArCh (Vercel → Project → Settings → Environment Variables):
  ARCHHUB_URL=$hub
  ARCHHUB_TOKEN=$(token_for simple-arch)

ArchMAP (no shell onde roda 'archmap serve'):
  export ARCHHUB_URL=$hub
  export ARCHHUB_TOKEN=$(token_for archmap)
EOF
}

case "${1:-}" in
  tokens) cmd_tokens ;;
  secrets) cmd_secrets "${2:-}" ;;
  check) cmd_check "${2:-}" ;;
  publish) cmd_publish "${2:-}" ;;
  runtime) cmd_runtime "${2:-}" ;;
  *) sed -n '2,13p' "$0"; exit 1 ;;
esac

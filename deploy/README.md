# Deploy do Arch Hub + secrets da linha — checklist

Objetivo: o Hub no ar num endereço **público** e os 4 repositórios publicando
o score de arquitetura nele pelo CI. Tempo estimado: 20–30 min.

O que já está pronto no repositório:

| Arquivo                              | Para quê                                                 |
|--------------------------------------|----------------------------------------------------------|
| `Dockerfile`                         | Imagem do Hub (JRE 21, não-root, volume `/app/data`)     |
| `render.yaml`                        | Opção A — deploy no Render (grátis)                      |
| `deploy/self-host/docker-compose.yml`| Opção B — sua máquina ou VPS (túnel Cloudflare ou Caddy) |
| `scripts/arch-line-setup.sh`         | Tokens, secrets dos 4 repos, verificação e publicação    |

## 0. Pré-requisitos (no PC)

```bash
gh --version && gh auth login     # GitHub CLI, logado na conta Kaua-KGzin
git clone https://github.com/Kaua-KGzin/System-Arch && cd System-Arch
```

Opção B também precisa de Docker (`docker compose version`).

## 1. Gerar os tokens

```bash
scripts/arch-line-setup.sh tokens
```

Gera um token por sistema em `.arch-line/tokens.env` (fora do git) e imprime a
linha `ARCHHUB_SYSTEM_TOKENS=…`. Guarde essa linha para o passo 2. Rodar de novo
reaproveita os mesmos tokens.

## 2. Subir o Hub (escolha uma opção)

### Opção A — Render (recomendada para começar: grátis, HTTPS, sem servidor)

1. https://dashboard.render.com → **New → Blueprint** → conecte o GitHub e escolha `System-Arch`.
2. O Render lê o `render.yaml` e pede as variáveis marcadas como secretas:
   - `ARCHHUB_SYSTEM_TOKENS` → cole a linha do passo 1 (só o valor, depois do `=`).
   - `ARCHHUB_SECURITY_TOKEN` → opcional (token admin); pode deixar vazio.
   - `ARCHHUB_PUBLIC_URL` → deixe vazio por enquanto.
3. **Apply**. O primeiro build leva uns 5 min. A URL fica tipo `https://arch-hub-xxxx.onrender.com`.
4. Volte em **Environment**, preencha `ARCHHUB_PUBLIC_URL` com essa URL e salve (redeploy automático).

Limites do plano free: o serviço dorme após ~15 min sem tráfego (a primeira
chamada demora ~1 min) e o disco não persiste. Na prática: os sistemas se
re-registram sozinhos e os scores voltam no próximo `Arch line` (toda segunda e
a cada push no `main`). Se incomodar, o plano pago com disco resolve.

### Opção B — sua máquina ou VPS (Docker)

```bash
cd deploy/self-host
cp .env.example .env    # cole ARCHHUB_SYSTEM_TOKENS (passo 1)
```

Escolha como expor:

- **Cloudflare Tunnel** (sem abrir portas, URL fixa; precisa de um domínio na Cloudflare):
  one.dash.cloudflare.com → Networks → Tunnels → Create → copie o token para
  `CLOUDFLARE_TUNNEL_TOKEN` no `.env` → em *Public Hostname* aponte
  `hub.seudominio.com` para `http://hub:8080`.
  ```bash
  docker compose --profile tunnel up -d --build
  ```
- **VPS com domínio** (HTTPS automático): aponte o DNS do domínio para o IP da VPS,
  preencha `HUB_DOMAIN` no `.env`.
  ```bash
  docker compose --profile caddy up -d --build
  ```

Preencha `ARCHHUB_PUBLIC_URL` no `.env` com a URL final e rode o `up` de novo.

> Túnel "rápido" (`cloudflared tunnel --url`, ngrok sem domínio fixo) funciona para
> testar, mas a URL muda a cada reinício — e aí o secret `ARCHHUB_URL` fica velho.

## 3. Conferir o Hub e os tokens

```bash
scripts/arch-line-setup.sh check https://SEU-HUB
```

Esperado: `Hub no ar` e `token de … aceito` para os 4 sistemas. `recusado (HTTP 401)`
= a linha `ARCHHUB_SYSTEM_TOKENS` no Hub não bate com `.arch-line/tokens.env`.

## 4. Gravar os 8 secrets (4 repos × 2)

```bash
scripts/arch-line-setup.sh secrets https://SEU-HUB
```

Grava `ARCHHUB_URL` e o `ARCHHUB_TOKEN` certo de cada repositório via `gh`
(os valores vão por stdin, nunca na linha de comando).

## 5. Publicar e ver os scores

```bash
scripts/arch-line-setup.sh publish https://SEU-HUB
```

Dispara o workflow **Arch line** nos 4 repos, espera ~2,5 min e lista os scores.
Abra `https://SEU-HUB` → aba **Arquitetura** para ver no dashboard.

## 6. (Opcional) Registro em runtime

```bash
scripts/arch-line-setup.sh runtime https://SEU-HUB
```

Mostra o que pôr no `.env` do PVD, nas Environment Variables da Vercel
(SIMPLE-ArCh) e no shell do `archmap serve` para eles aparecerem **UP** no Hub.

## Problemas comuns

| Sintoma                                   | Causa / solução                                                        |
|-------------------------------------------|-------------------------------------------------------------------------|
| `check`: Hub não respondeu                | Render dormindo (espere 1 min e repita) ou URL errada                  |
| `check`: token recusado (401)             | `ARCHHUB_SYSTEM_TOKENS` no Hub diferente do `tokens.env`               |
| Job do CI: "ARCHHUB_URL not set"          | Secret faltando no repo — rode o passo 4                                |
| Job do CI: "rejected (HTTP 403)"          | `ARCHHUB_TOKEN` de outro sistema no repo — rode o passo 4 de novo       |
| Scores somem no Render free               | Disco efêmero; voltam no próximo `Arch line`                           |

## Fazendo junto com o Claude Code

No PC, dentro do clone do `System-Arch`:

```bash
claude
```

e peça: *"siga o deploy/README.md: Hub no Render e secrets nos 4 repos"*. O
Claude roda os passos 1, 3, 4 e 5. O passo 2 depende de você (login no Render
ou na Cloudflare, no navegador).

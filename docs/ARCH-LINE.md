# Arch Line — contrato da linha

A **linha Arch** é a família de sistemas do Arch Lab. Cada sistema continua
independente — com stack, repositório, deploy e propósito próprios — e todos
falam o mesmo **protocolo mínimo** para aparecer no Arch Hub como partes de
um mesmo ecossistema.

> Princípio: integração **opcional e não intrusiva**. Sem `ARCHHUB_URL`
> configurada, nenhum sistema muda de comportamento. Com o Hub fora do ar,
> nenhum sistema cai, trava ou atrasa o boot.

## Os sistemas

| id            | Repositório   | Papel (`role`)  | O que é                                          | Stack                      | Porta padrão | Health          |
|---------------|---------------|-----------------|--------------------------------------------------|----------------------------|--------------|-----------------|
| `arch-hub`    | System-Arch   | `platform`      | Catálogo central, status e grafo de conexões     | Java 21 / Spring Boot      | 8080         | `/actuator/health` |
| `system-pvd`  | System-PVD    | `business`      | ARCHNEXUS — ERP & PDV                            | .NET 10 / React / Postgres | 8080 (container) · 5235 (dev) | `/health` |
| `simple-arch` | SIMPLE-ArCh   | `collaboration` | Boards, tasks e sync com GitHub para times de dev | NestJS / React / Postgres  | 3000         | `/health`       |
| `archmap`     | ArchMAP       | `insight`       | Análise estática de arquitetura (CLI + web UI)   | Python (stdlib)            | 3000 (`serve`) | `/api/health` |

### Papéis

- **platform** — infraestrutura comum da linha (o Hub).
- **business** — produto de negócio para o usuário final.
- **collaboration** — ferramenta de trabalho do time.
- **insight** — ferramenta que analisa/observa os demais.

O papel vai na tag `role:<papel>`; todo sistema da linha também leva a tag
`arch-line` — no dashboard, filtre por ela para ver só a linha.

### Rodando tudo na mesma máquina

As portas padrão colidem (Hub × PVD em 8080; SIMPLE-ArCh × ArchMAP em 3000).
Mapa sugerido para rodar a linha inteira junto:

| Sistema     | Porta | Como                                              |
|-------------|-------|---------------------------------------------------|
| Arch Hub    | 8080  | `mvn spring-boot:run`                             |
| System-PVD  | 8081  | `PVD_HTTP_PORT=8081 docker compose up`            |
| SIMPLE-ArCh | 3000  | `node dist/main.js`                               |
| ArchMAP     | 3100  | `archmap serve <projeto> --port 3100`             |

## Protocolo v1

### 1. Variáveis de ambiente (iguais em todos os sistemas)

| Variável                    | Obrigatória | Significado                                                                 |
|-----------------------------|-------------|------------------------------------------------------------------------------|
| `ARCHHUB_URL`               | não         | URL do Hub (ex. `http://localhost:8080`). **Vazia = integração desligada.**  |
| `ARCHHUB_TOKEN`             | não         | Enviado em `X-Hub-Token` quando o Hub exige token (`ARCHHUB_SECURITY_TOKEN`). |
| `ARCHHUB_PUBLIC_URL`        | não         | Endereço pelo qual o Hub alcança **este** sistema. Default: o local.         |
| `ARCHHUB_HEARTBEAT_SECONDS` | não         | Período do heartbeat (default `20`, `0` = só registra).                      |

O default de 20s fica abaixo da janela `stale-after` do Hub (45s), então um
heartbeat perdido não derruba o status.

### 2. Ciclo de vida do adaptador

```
boot ──► servidor escutando ──► POST /api/v1/systems  (registro / upsert)
                                   │ falhou? backoff 2s, 4s, 8s… até o período do heartbeat
                                   ▼
                          a cada N s: POST /api/v1/systems/{id}/heartbeat
                                   │ 404? (Hub reiniciou sem persistência)
                                   └──► volta ao registro
```

Regras:

1. Registrar **só depois** que o servidor está escutando (o Hub pode pingar o health logo em seguida).
2. Todo erro de rede/HTTP vira **log de aviso**, nunca exceção para o host.
3. Timeout de requisição curto (5s).
4. Payload de registro:

```json
{
  "id": "simple-arch",
  "name": "SIMPLE ArCh",
  "baseUrl": "http://localhost:3000",
  "healthCheckUrl": "http://localhost:3000/health",
  "description": "…",
  "tags": ["arch-line", "role:collaboration", "nestjs", "react"],
  "connectsTo": ["arch-hub"]
}
```

`connectsTo` lista só dependências **reais em runtime**. Hoje todos chamam
apenas o Hub; quando um sistema passar a consumir outro (ex. SIMPLE-ArCh
mostrando o score do ArchMAP), adicione o id aqui e o grafo do Hub atualiza
sozinho.

### 3. Serverless e desktop

- **Serverless (Vercel)**: não há processo longo para heartbeat. O adaptador
  registra a cada cold start e o **health check ativo do Hub** mantém o status.
  O Hub precisa conseguir alcançar a URL pública.
- **Desktop / atrás de NAT**: o Hub não alcança o health check, então o
  heartbeat é o que mantém o status `UP`.

### 4. Manifesto `arch.json`

Cada repositório tem um `arch.json` na raiz descrevendo seu lugar na linha
(validado por [`arch-line.schema.json`](./arch-line.schema.json)). Ele é a
fonte de verdade estática — id, papel, porta, health e onde fica o adaptador —
e serve para ferramentas (CI, scripts, o próprio ArchMAP) descobrirem a linha
sem precisar do Hub no ar.

## Adaptadores

| Sistema     | Arquivo                                                        |
|-------------|----------------------------------------------------------------|
| Arch Hub    | `src/main/java/dev/kauakgzin/archhub/service/SelfRegistration.java` (auto-registro) |
| System-PVD  | `Archlab.Backend/Integrations/ArchHub/ArchHubRegistrar.cs`     |
| SIMPLE-ArCh | `src/integrations/arch-hub.service.ts`                         |
| ArchMAP     | `src/archmap/cli/arch_hub.py` (ativo em `archmap serve`)       |

## Adicionando um novo sistema à linha

1. Escolha um `id` (slug) e um `role`.
2. Crie o `arch.json` na raiz.
3. Exponha um endpoint de health (HTTP 2xx = vivo).
4. Implemente o adaptador seguindo o ciclo acima (≈100 linhas em qualquer stack).
5. Adicione a linha na tabela "Os sistemas" deste documento.

## Evolução prevista

- **Identidade visual comum** (paleta/logo "Arch") compartilhada entre as UIs.
- **Token por sistema** no Hub, em vez do segredo global.
- **ArchMAP como CI da linha**: rodar `archmap check` nos 4 repositórios e
  publicar o score de arquitetura no Hub (campo extra no registro).
- **SSO da linha**: hoje cada sistema tem seu próprio login.

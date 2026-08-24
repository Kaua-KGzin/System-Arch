# Arch Hub

Painel central de controle e conexão entre os sistemas do **Arch Lab**
(`System-PVD`, `ArchMAP`, `SIMPLE-ArCh` e futuros sistemas), similar a um
catálogo de serviços interno (no estilo do que a Google usa para saber
"o que existe e o que está no ar" na sua infra).

Cada sistema se **registra** no Hub informando quem é, onde vive e com
quem se conecta. O Hub mantém o catálogo, verifica periodicamente se cada
um continua respondendo, registra o histórico de atividade e expõe tudo
via API REST e um dashboard web — servindo como ponto único de gestão dos
sistemas do laboratório.

## Stack

- Java 21
- Spring Boot 3.3 (Web, Validation, Actuator, springdoc-openapi)
- Maven

## Rodando localmente

```bash
mvn spring-boot:run
```

- Dashboard: `http://localhost:8080`
- API: `http://localhost:8080/api/v1/...`
- Documentação interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`

## Funcionalidades

- **Registro central**: cada sistema se anuncia ao Hub (nome, URL, tags, descrição).
- **Conexões entre sistemas**: cada sistema pode declarar quais outros
  sistemas ele acessa, formando um grafo de dependências visualizado no
  dashboard.
- **Monitoramento de status**: health check ativo (ping periódico) e/ou
  heartbeat manual, com status `UP` / `DOWN` / `UNKNOWN` calculado em
  tempo real.
- **Feed de atividade**: log dos últimos eventos (registro, remoção,
  sistema caiu, sistema voltou).
- **Busca e filtros**: por tag, status ou texto livre (nome/id/descrição).
- **Estatísticas agregadas**: contagem de sistemas por status e total de conexões.
- **Autenticação opcional**: token compartilhado para proteger operações
  de escrita (registro/heartbeat/remoção), desligado por padrão.
- **Persistência em disco**: o catálogo sobrevive a reinícios do Hub
  (snapshot JSON, sem precisar de banco de dados).
- **Documentação OpenAPI/Swagger** gerada automaticamente.
- **Acesso pelo celular**: o dashboard detecta o IP da rede local e mostra
  um QR code — basta escanear para abrir no celular.
- **Pacotes nativos** (Windows `.exe`, macOS `.dmg`, Linux `.deb`/portátil),
  com JRE embutida — quem for só usar não precisa instalar Java.

## Como um sistema se conecta ao Hub

Qualquer sistema do ecossistema (System-PVD, ArchMAP, SIMPLE-ArCh, ou um
novo) se registra com um `POST` simples, tipicamente disparado na
inicialização do serviço:

```bash
curl -X POST http://localhost:8080/api/v1/systems \
  -H "Content-Type: application/json" \
  -d '{
        "id": "system-pvd",
        "name": "System PVD",
        "baseUrl": "http://localhost:5000",
        "healthCheckUrl": "http://localhost:5000/health",
        "description": "Backend + Desktop do laboratorio Arch",
        "tags": ["dotnet", "desktop"],
        "connectsTo": ["archmap"]
      }'
```

- `id`: slug estável (minúsculo, sem espaços) usado como identificador único.
- `baseUrl`: onde o sistema pode ser acessado.
- `healthCheckUrl` (opcional): endpoint que o Hub vai pingar periodicamente
  para saber se o sistema está de pé. Se o sistema não expõe um endpoint de
  health check (ex.: um app desktop), omita esse campo e envie heartbeats
  manuais em vez disso (veja abaixo).
- `connectsTo` (opcional): ids de outros sistemas que este consome/acessa.
  Não precisam estar registrados ainda — o grafo mostra a conexão como
  pendente até o alvo aparecer.
- Registrar de novo com o mesmo `id` atualiza os dados (upsert) sem perder
  a data do primeiro registro.

### Heartbeat manual

Para sistemas que não podem ser "pingados" de fora (ex.: um processo
desktop atrás de NAT), envie um heartbeat periódico:

```bash
curl -X POST http://localhost:8080/api/v1/systems/system-pvd/heartbeat
```

### Status

O status de cada sistema é calculado, não armazenado diretamente:

| Status    | Significado                                                        |
|-----------|---------------------------------------------------------------------|
| `UP`      | Recebemos um sinal (health check ou heartbeat) dentro da janela.   |
| `DOWN`    | Já se registrou, mas não dá sinal há mais tempo que o permitido.   |
| `UNKNOWN` | Registrado, mas ainda nenhum sinal foi recebido.                   |

A janela de tolerância (`archhub.health.stale-after`, padrão 45s) e o
intervalo de checagem ativa (`archhub.health.check-interval-ms`, padrão
15s) são configuráveis em `application.yml`.

## API

| Método | Rota                              | Descrição                                  |
|--------|------------------------------------|----------------------------------------------|
| POST   | `/api/v1/systems`                  | Registra ou atualiza um sistema               |
| GET    | `/api/v1/systems`                  | Lista sistemas (filtros: `tag`, `status`, `q`)|
| GET    | `/api/v1/systems/{id}`             | Detalhe de um sistema                          |
| POST   | `/api/v1/systems/{id}/heartbeat`   | Heartbeat manual                               |
| DELETE | `/api/v1/systems/{id}`             | Remove um sistema do catálogo                  |
| GET    | `/api/v1/connections`              | Grafo de conexões declaradas entre sistemas    |
| GET    | `/api/v1/stats`                    | Contagem por status + total de conexões        |
| GET    | `/api/v1/events?limit=50`          | Feed de atividade (mais recentes primeiro)     |

Todas as rotas `GET` são públicas. `POST`/`DELETE` em `/api/v1/systems/**`
exigem o header `X-Hub-Token` **somente se** `archhub.security.token`
estiver configurado (veja abaixo).

## Segurança (opcional)

Por padrão o Hub fica aberto (ambiente de laboratório). Para exigir um
token nas operações de escrita, defina a variável de ambiente:

```bash
ARCHHUB_SECURITY_TOKEN=um-segredo-forte mvn spring-boot:run
```

E envie o header em toda chamada de registro/heartbeat/remoção:

```bash
curl -X POST http://localhost:8080/api/v1/systems \
  -H "X-Hub-Token: um-segredo-forte" \
  -H "Content-Type: application/json" \
  -d '{"id": "system-pvd", "name": "System PVD", "baseUrl": "http://localhost:5000"}'
```

## Persistência

O catálogo é salvo em `data/systems.json` ao desligar o Hub e recarregado
na próxima inicialização (`archhub.persistence.enabled`, ligado por
padrão). Não é um banco de dados — é um snapshot simples, suficiente para
o Hub não esquecer o catálogo entre reinícios locais.

## Acesso pelo celular

O Hub sobe escutando em todas as interfaces de rede (não só `localhost`),
então já é acessível de qualquer dispositivo na mesma rede. Para facilitar,
o dashboard mostra automaticamente um botão **"📱 Ver no celular"** com:

- o(s) endereço(s) de rede local detectados (`GET /api/v1/network`);
- um QR code gerado no servidor (`GET /api/v1/network/qr.svg`), sem depender
  de nenhuma biblioteca externa no navegador — basta escanear com a câmera
  do celular para abrir o dashboard.

O dashboard também é responsivo (grade de cards, abas com scroll horizontal,
layout empilhado em telas pequenas) e pode ser "instalado" na tela inicial
do celular como um atalho (PWA — `manifest.json` + ícones em `static/icons/`).

Se nenhum endereço de rede local for encontrado (ex.: um container isolado
com apenas loopback), o botão simplesmente não aparece.

## Pacotes nativos (sem precisar instalar Java)

Assim como o `ArchNexus.exe` do System-PVD e o `archmap.exe` do ArchMAP, o
Arch Hub pode ser empacotado como um executável nativo autocontido — usando
[`jpackage`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
(inclui a própria JRE, então quem for só rodar não precisa instalar Java):

```bash
# Linux/macOS — produz um app-image portátil + instalador nativo (.deb/.dmg)
./scripts/build-package.sh

# Windows (PowerShell) — produz um app-image + instalador .exe (requer WiX Toolset)
powershell -ExecutionPolicy Bypass -File scripts\build-exe.ps1
```

Os artefatos saem em `dist-desktop/`. O binário aceita `--version` para um
smoke test rápido sem subir o servidor inteiro (`ArchHub --version`).

O workflow `.github/workflows/release.yml` builda os três pacotes (Windows,
macOS, Linux) em CI e anexa numa release do GitHub sempre que uma tag
`vX.Y.Z` é publicada (a versão precisa bater com a do `pom.xml` — rode
`mvn versions:set -DnewVersion=X.Y.Z` antes de taguear). Sem tag, o mesmo
workflow pode ser disparado manualmente (`Run workflow`) só para gerar os
artifacts de teste.

## Estrutura do projeto

```
src/main/java/dev/kauakgzin/archhub/
├── ArchHubApplication.java   # main() também responde a --version
├── config/         # RestClient, Clock, OpenAPI e propriedades (health/security/persistence)
├── domain/         # RegisteredSystem, SystemStatus, SystemEvent, EventType
├── exception/
├── persistence/     # Snapshot em disco (SystemSnapshot, PersistenceService)
├── repository/      # SystemRegistry (catálogo em memória), EventLog (feed de atividade)
├── service/          # RegistrationService, HealthCheckService, StatusMonitor,
│                      # NetworkInfoService, QrCodeService (acesso pelo celular)
└── web/               # Controllers REST, DTOs, filtro de autenticação, exception handler
src/main/resources/
├── application.yml
└── static/            # Dashboard (HTML/CSS/JS vanilla), manifest.json e ícones (PWA)
scripts/
├── build-package.sh   # Empacota para Linux/macOS (jpackage)
└── build-exe.ps1      # Empacota para Windows (jpackage + WiX)
.github/workflows/
├── ci.yml             # Testes em todo push/PR
└── release.yml        # Builda e publica os pacotes nativos em tags vX.Y.Z
```

## Próximos passos (roadmap)

- Adaptadores de registro automático para os demais repositórios
  (`System-PVD`, `ArchMAP`, `SIMPLE-ArCh`) disparando o `POST` de
  registro na própria inicialização de cada um.
- Webhooks/notificações externas quando um sistema muda de status.
- Autenticação por sistema (um token por serviço, não apenas um segredo global).
- Trocar o snapshot em disco por um banco leve (ex. H2/SQLite) se o
  catálogo crescer muito.

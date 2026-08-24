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

## Estrutura do projeto

```
src/main/java/dev/kauakgzin/archhub/
├── ArchHubApplication.java
├── config/         # RestClient, Clock, OpenAPI e propriedades (health/security/persistence)
├── domain/         # RegisteredSystem, SystemStatus, SystemEvent, EventType
├── exception/
├── persistence/     # Snapshot em disco (SystemSnapshot, PersistenceService)
├── repository/      # SystemRegistry (catálogo em memória), EventLog (feed de atividade)
├── service/          # RegistrationService, HealthCheckService, StatusMonitor
└── web/               # Controllers REST, DTOs, filtro de autenticação, exception handler
src/main/resources/
├── application.yml
└── static/            # Dashboard (HTML/CSS/JS vanilla): sistemas, grafo de conexões, atividade
```

## Próximos passos (roadmap)

- Adaptadores de registro automático para os demais repositórios
  (`System-PVD`, `ArchMAP`, `SIMPLE-ArCh`) disparando o `POST` de
  registro na própria inicialização de cada um.
- Webhooks/notificações externas quando um sistema muda de status.
- Autenticação por sistema (um token por serviço, não apenas um segredo global).
- Trocar o snapshot em disco por um banco leve (ex. H2/SQLite) se o
  catálogo crescer muito.

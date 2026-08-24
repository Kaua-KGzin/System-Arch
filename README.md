# Arch Hub

Serviço central que conecta os sistemas do ecossistema **Arch**
(`System-PVD`, `ArchMAP`, `SIMPLE-ArCh` e futuros sistemas) em um único
painel, similar a um catálogo de serviços interno (no estilo do que a
Google usa para saber "o que existe e o que está no ar" na sua infra).

Cada sistema se **registra** no Hub informando quem é e onde vive; o Hub
mantém um catálogo em memória e verifica periodicamente se cada um
continua respondendo, expondo tudo isso via API REST e um dashboard web.

## Stack

- Java 21
- Spring Boot 3.3 (Web, Validation, Actuator)
- Maven

## Rodando localmente

```bash
mvn spring-boot:run
```

O dashboard fica em `http://localhost:8080` e a API em
`http://localhost:8080/api/v1/systems`.

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
        "tags": ["dotnet", "desktop"]
      }'
```

- `id`: slug estável (minúsculo, sem espaços) usado como identificador único.
- `baseUrl`: onde o sistema pode ser acessado.
- `healthCheckUrl` (opcional): endpoint que o Hub vai pingar periodicamente
  para saber se o sistema está de pé. Se o sistema não expõe um endpoint de
  health check (ex.: um app desktop), omita esse campo e envie heartbeats
  manuais em vez disso (veja abaixo).
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

| Método | Rota                              | Descrição                          |
|--------|------------------------------------|-------------------------------------|
| POST   | `/api/v1/systems`                  | Registra ou atualiza um sistema     |
| GET    | `/api/v1/systems`                  | Lista todos os sistemas conectados  |
| GET    | `/api/v1/systems/{id}`             | Detalhe de um sistema                |
| POST   | `/api/v1/systems/{id}/heartbeat`   | Heartbeat manual                     |
| DELETE | `/api/v1/systems/{id}`             | Remove um sistema do catálogo        |

## Estrutura do projeto

```
src/main/java/dev/kauakgzin/archhub/
├── ArchHubApplication.java
├── config/         # RestClient, Clock e archhub.health.* properties
├── domain/         # RegisteredSystem, SystemStatus
├── exception/
├── repository/     # SystemRegistry (catálogo em memória)
├── service/        # RegistrationService, HealthCheckService (agendado)
└── web/            # Controllers REST, DTOs, exception handler
src/main/resources/
├── application.yml
└── static/         # Dashboard (HTML/CSS/JS vanilla)
```

## Próximos passos (roadmap)

- Persistência do catálogo (hoje é em memória — reinicia zerado).
- Autenticação entre sistemas (token por serviço) para o registro.
- Emitir eventos (ex.: webhook) quando um sistema muda de status.
- Adaptadores de registro automático para os demais repositórios
  (`System-PVD`, `ArchMAP`, `SIMPLE-ArCh`) disparando o `POST` de
  registro na própria inicialização de cada um.

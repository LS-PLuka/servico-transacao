# servico-transacao

Microsserviço responsável por receber, validar e registrar transações financeiras. É o ponto de entrada do [Motor de Análise de Risco de Transações](https://github.com/LS-PLuka/antifraud-system) — um sistema antifraude construído com arquitetura de microsserviços.

<p align="center">
  <img src="../servico-transacao/src/main/java/antifraud/servicotransacao/docs/banner.png" alt="Serviço de Transação" width="100%">
</p>

---

## O que este serviço faz

- Recebe transações financeiras via API REST
- Valida todos os campos de entrada com mensagens de erro claras
- Persiste a transação no banco de dados com status `PENDENTE`
- Publica um evento no RabbitMQ para que o `motor-risco` processe a análise de risco
- Expõe endpoints de consulta por ID e por conta, com controle de acesso por perfil

Este serviço não analisa risco. Sua responsabilidade é receber, validar, registrar e repassar.

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.x | Framework base |
| Spring Security | 3.x | Autenticação e autorização |
| Spring Data JPA | 3.x | Persistência no banco de dados |
| Spring AMQP | 3.x | Integração com RabbitMQ |
| PostgreSQL | 16 | Banco de dados relacional |
| RabbitMQ | 3.x | Fila de mensagens |
| JWT | - | Autenticação stateless |
| SpringDoc OpenAPI | 2.x | Documentação da API (Swagger) |
| JUnit + Mockito | 5.x | Testes unitários |
| Testcontainers | 1.x | Testes de integração |
| Docker | - | Containerização |

---

## Pré-requisitos

- Java 21
- Docker e Docker Compose instalados
- Maven 3.9+

---

## Como rodar localmente

### Opção 1 — Via repositório principal (recomendado)

Clone o repositório principal e suba todos os serviços juntos:

```bash
git clone https://github.com/LS-PLuka/antifraud-system
cd antifraud-system
docker compose up --build
```

### Opção 2 — Somente este serviço

Suba as dependências necessárias:

```bash
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=antifraude \
  -p 5432:5432 postgres:16

docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

Clone e rode o serviço:

```bash
git clone https://github.com/LS-PLuka/servico-transacao
cd servico-transacao
./mvnw spring-boot:run
```

---

## Variáveis de ambiente

| Variável | Valor padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/antifraude` | URL do PostgreSQL |
| `DB_USERNAME` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `JWT_SECRET` | — | Chave secreta para assinar tokens JWT (obrigatória) |
| `JWT_EXPIRACAO_HORAS` | `24` | Tempo de expiração do token em horas |

---

## Endpoints

A documentação interativa completa está disponível via Swagger após subir o serviço:

```text
http://localhost:8080/swagger-ui.html
```

Resumo dos endpoints:

| Método | Endpoint | Autenticação | Perfil | Descrição |
|---|---|---|---|---|
| POST | `/auth/login` | Não | — | Autenticar e receber token JWT |
| POST | `/transacoes` | Sim | ADMIN | Registrar nova transação |
| GET | `/transacoes/{id}` | Sim | ADMIN, USUARIO | Buscar transação por ID |
| GET | `/transacoes/conta/{contaId}` | Sim | ADMIN, USUARIO | Listar transações de uma conta |

**Como usar o token:**

Após o login, inclua o token no cabeçalho de todas as requisições:

```text
Authorization: Bearer <seu_token_aqui>
```

---

## Fluxo de uma transação

```text
Cliente
│
▼ POST /transacoes
servico-transacao
├── Valida os campos
├── Salva no PostgreSQL com status PENDENTE
└── Publica evento na fila transacoes.analise
│
▼ RabbitMQ
motor-risco (repositório separado)
```

---

## Estrutura do projeto

```text
src/
├── main/java/com/antifraude/transacao/
│   ├── config/          # Configurações (RabbitMQ, Security)
│   ├── controller/      # Endpoints da API REST
│   ├── dto/             # Objetos de transferência de dados
│   ├── entity/          # Entidades do banco de dados
│   ├── enums/           # Enumerações (status, perfis)
│   ├── exception/       # Tratamento centralizado de erros
│   ├── messaging/       # Publicação de eventos no RabbitMQ
│   ├── repository/      # Acesso ao banco de dados
│   ├── security/        # JWT e filtro de autenticação
│   └── service/         # Regras de negócio
└── test/
    ├── controller/      # Testes dos endpoints
    ├── service/         # Testes das regras de negócio
    └── integration/     # Testes de integração com Testcontainers
```

---

## Como rodar os testes

```bash
./mvnw test
```

Os testes de integração sobem instâncias reais do PostgreSQL e do RabbitMQ via Docker automaticamente através do Testcontainers. O Docker precisa estar rodando na máquina.

---

## Parte do sistema

Este repositório é um dos microsserviços do **Motor de Análise de Risco de Transações**.

| Repositório | Descrição |
|---|---|
| [antifraud-system](https://github.com/LS-PLuka/antifraud-system) | Repositório principal — Docker Compose e documentação geral |
| [servico-transacao](https://github.com/LS-PLuka/servico-transacao) | **Este repositório** — entrada de transações |
| [motor-risco](https://github.com/LS-PLuka/motor-risco) | Engine de análise de risco com cadeia de regras |
| [servico-auditoria](https://github.com/LS-PLuka/servico-auditoria) | Histórico de decisões e auditoria |

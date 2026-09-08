# servico-transacao

[![CI](https://github.com/LS-PLuka/servico-transacao/actions/workflows/ci.yml/badge.svg)](https://github.com/LS-PLuka/servico-transacao/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=flat-square)

Ponto de entrada de um sistema antifraude distribuído. Recebe transações financeiras via API REST, valida, persiste com status `PENDENTE` e publica um evento no RabbitMQ para que o motor de risco decida — de forma assíncrona, sem bloquear a resposta ao cliente.

Este serviço **não analisa risco**. A responsabilidade dele é receber, validar, registrar e repassar. Essa fronteira é deliberada e é o que permite que a análise evolua sem tocar no caminho crítico da transação.

<p align="center">
  <img src="docs/banner.png" alt="Serviço de Transação" width="100%">
</p>

---

## Índice

- [Como rodar em 2 comandos](#como-rodar-em-2-comandos)
- [Arquitetura](#arquitetura)
- [Modelo de acesso](#modelo-de-acesso)
- [API](#api)
- [Contrato de erro](#contrato-de-erro)
- [Evento publicado](#evento-publicado)
- [Decisões de arquitetura e trade-offs](#decisões-de-arquitetura-e-trade-offs)
- [Testes](#testes)
- [Limitações conhecidas](#limitações-conhecidas)
- [Stack](#stack)
- [Configuração](#configuração)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Como rodar em 2 comandos

Requisitos: Docker e Docker Compose. Nada mais — o Maven vem no wrapper e o build acontece dentro do container.

```bash
git clone https://github.com/LS-PLuka/servico-transacao
cd servico-transacao && docker compose up --build
```

O Compose sobe PostgreSQL, RabbitMQ e a aplicação, com `healthcheck` nos dois primeiros e `depends_on: service_healthy` no terceiro — a API só inicia quando o banco aceita conexão e o broker responde ping.

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| RabbitMQ Management | http://localhost:15672 (`guest` / `guest`) |

### Primeiro acesso

Um usuário `ADMIN` é criado automaticamente no boot, caso ainda não exista (`DadosAdminInicial`). Sem ele, os endpoints `/admin/**` seriam inalcançáveis — é o problema clássico do ovo e da galinha em bootstrap de dados.

```
E-mail: admin@antifraude.com
Senha:  admin123456
```

> Credenciais de **desenvolvimento local**. Em qualquer ambiente real, sobrescreva via `ADMIN_EMAIL` e `ADMIN_SENHA`.

Para operar como usuário comum, registre-se em `POST /auth/registro` — o perfil é forçado para `USUARIO` no service, então ninguém se auto-promove a administrador.

### Rodando a aplicação fora do container

Útil para debug na IDE. Sobe apenas as dependências:

```bash
docker compose up postgres rabbitmq
./mvnw spring-boot:run
```

---

## Arquitetura

```
                    ┌──────────────────────────────────────────┐
   POST             │            servico-transacao             │
   /transacoes/     │                                          │
   efetuar   ──────▶│  JwtAuthFilter                           │
                    │       │ valida assinatura, carrega user   │
                    │       ▼                                  │
                    │  TransacaoController                     │
                    │       │ @Valid no DTO                     │
                    │       ▼                                  │
                    │  TransacaoService                        │
                    │       │ 1. perfil é USUARIO?              │
                    │       │ 2. contaId é do próprio usuário?  │
                    │       ▼                                  │
                    │  ┌─────────────┐   ┌─────────────────┐   │
                    │  │ PostgreSQL  │   │ TransacaoPublisher│ │
                    │  │  PENDENTE   │   │                 │   │
                    │  └─────────────┘   └────────┬────────┘   │
                    │        ▲                    │            │
                    │        └── @Transactional ──┘            │
                    └─────────────────────────────┼────────────┘
                                                  │
                              transacoes.exchange │ (direct)
                                                  ▼
                                   routing key: transacoes.risco
                                                  │
                                                  ▼
                                    fila: transacoes.analise (durable)
                                                  │
                                                  ▼
                                      motor-risco (repo separado)
```

O `save` e o `publish` acontecem dentro do mesmo `@Transactional`. Se a publicação falhar, o insert sofre rollback e o cliente recebe `500` — a transação não fica registrada sem que o motor de risco tenha sido notificado. O trade-off dessa escolha está detalhado [abaixo](#3-o-evento-é-publicado-dentro-da-transação).

---

## Modelo de acesso

Duas dimensões controlam cada requisição: o **perfil** (`USUARIO` ou `ADMIN`, via `ROLE_` no Spring Security) e a **posse da conta** (o `contaId` pertence ao usuário autenticado?). A segunda é verificada em código, no service, comparando com o principal do `SecurityContext`.

| Ação | USUARIO (própria conta) | USUARIO (outra conta) | ADMIN |
|---|---|---|---|
| Efetuar transação | `201` | `403` | `403` |
| Buscar transação por ID | `200` | `403` | `200` |
| Listar transações da conta | `200` | `403` | `200` |
| Endpoints `/admin/**` | `403` | `403` | `200` / `201` |

O `403` para `ADMIN` em *efetuar transação* é intencional, não um bug — ver [segregação de funções](#2-admin-não-transaciona).

---

## API

Documentação interativa completa no Swagger UI, com botão **Authorize** para colar o JWT (`persist-authorization` habilitado, o token sobrevive ao refresh).

### Autenticação — `/auth`

| Método | Rota | Auth | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/auth/registro` | pública | `201` | `400`, `409` |
| POST | `/auth/login` | pública | `200` | `400`, `401` |

```http
POST /auth/login
Content-Type: application/json

{ "email": "usuario@exemplo.com", "senha": "senha123" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "perfil": "USUARIO"
}
```

O token carrega `sub` (e-mail), `perfil`, `iat` e `exp`, assinado em HMAC-SHA256, válido por 24 h. Envie em todas as chamadas protegidas:

```
Authorization: Bearer <token>
```

### Transações — `/transacoes`

| Método | Rota | Auth | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/transacoes/efetuar` | `USUARIO`, própria conta | `201` | `400`, `401`, `403` |
| GET | `/transacoes/{id}/conta/{contaId}` | dono ou `ADMIN` | `200` | `401`, `403`, `404` |
| GET | `/transacoes/conta/{contaId}?pagina=0` | dono ou `ADMIN` | `200` | `401`, `403`, `404` |

```http
POST /transacoes/efetuar
Authorization: Bearer <token>
Content-Type: application/json

{
  "contaId": "550e8400-e29b-41d4-a716-446655440000",
  "valor": 150.75,
  "categoria": "RESTAURANTE",
  "codigoPais": "BRA",
  "dataHora": "2026-09-08T14:30:00"
}
```

```json
{
  "id": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "contaId": "550e8400-e29b-41d4-a716-446655440000",
  "valor": 150.75,
  "categoria": "RESTAURANTE",
  "codigoPais": "BRA",
  "status": "PENDENTE",
  "criadoEm": "2026-09-08T14:30:01"
}
```

Regras de validação: `valor` mínimo `0.01`; `codigoPais` exatamente 3 letras maiúsculas (`^[A-Z]{3}$`); todos os campos obrigatórios.

> **Nota de contrato:** o `dataHora` enviado é validado como obrigatório, mas o `criadoEm` persistido usa o instante de recebimento no servidor. Datas retroativas enviadas pelo cliente não são honradas — o servidor é a autoridade temporal.

`status` só assume `PENDENTE` neste serviço. Os valores `APROVADA`, `SINALIZADA` e `BLOQUEADA` existem no enum como vocabulário compartilhado, mas a transição é responsabilidade do `motor-risco`.

### Administração — `/admin/usuarios` (perfil `ADMIN`)

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| POST | `/admin/usuarios/registro` | `201` | `400`, `401`, `403`, `409` |
| GET | `/admin/usuarios/{id}` | `200` | `401`, `403`, `404` |
| GET | `/admin/usuarios?pagina=0` | `200` | `401`, `403` |

Diferente do auto-registro público, aqui o `perfil` vem no corpo — um `ADMIN` pode criar outro `ADMIN`.

### Paginação

Endpoints de listagem usam `?pagina=` (base 0), com página fixa de 10 itens, e respondem com envelope:

```json
{
  "conteudo": [ ... ],
  "paginaAtual": 0,
  "totalPaginas": 3,
  "totalItens": 27,
  "tamanhoPagina": 10
}
```

---

## Contrato de erro

Todas as exceções passam por um `@RestControllerAdvice` centralizado, em dois formatos previsíveis.

**Erro de negócio ou autenticação:**

```json
{ "status": 409, "erro": "Conflito", "mensagem": "E-mail já registrado" }
```

**Erro de validação** — um par campo/mensagem por violação:

```json
{
  "status": 400,
  "erro": "Erro de validação",
  "erros": {
    "valor": "O valor da transação deve ser maior que zero",
    "codigoPais": "O código do país deve ter exatamente 3 letras (ex: BRA, USA, ARG)"
  }
}
```

| Situação | Status | Exceção |
|---|---|---|
| Campo inválido ou ausente | `400` | `MethodArgumentNotValidException` |
| Token ausente, inválido ou expirado | `401` | `EntryPointNaoAutorizado` |
| Credenciais incorretas | `401` | `BadCredentialsException` |
| Perfil ou conta sem permissão | `403` | `AcessoNegadoException` / `AcessoNegadoHandler` |
| Transação ou usuário inexistente | `404` | `TransacaoNaoEncontradaException`, `UsuarioNaoEncontradoException` |
| E-mail já cadastrado | `409` | `EmailJaCadastradoException` |

Mensagens de credencial são genéricas (`"E-mail ou senha inválidos"`) de propósito: distinguir "usuário não existe" de "senha errada" entrega enumeração de contas a um atacante.

---

## Evento publicado

Topologia declarada em código (`RabbitMQConfig`), criada automaticamente no primeiro boot:

| Componente | Nome | Tipo |
|---|---|---|
| Exchange | `transacoes.exchange` | `direct` |
| Routing key | `transacoes.risco` | — |
| Fila | `transacoes.analise` | `durable` |

Payload (`TransacaoEventoDTO`, serializado via `Jackson2JsonMessageConverter`):

```json
{
  "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "contaId": "550e8400-e29b-41d4-a716-446655440000",
  "valor": 150.75,
  "categoria": "RESTAURANTE",
  "codigoPais": "BRA",
  "dataHora": "2026-09-08T14:30:01"
}
```

O evento **não carrega o `status`** — seria redundante, já que toda transação publicada está `PENDENTE` por definição. É o consumidor que decide o próximo estado.

---

## Decisões de arquitetura e trade-offs

### 1. A conta é o usuário

Não existe entidade `Conta`. O `contaId` de uma transação é o `id` do `Usuario`, e todo o controle de posse se apoia nessa igualdade.

**Por quê:** introduzir `Conta` como agregado separado exigiria relacionamento, ciclo de vida próprio e endpoints de gestão — complexidade sem retorno enquanto a regra é "um usuário, uma conta".

**Custo aceito:** o dia em que um usuário precisar de múltiplas contas, isso é uma migração de schema com mudança de contrato na API. A decisão é reversível, mas não de graça.

### 2. `ADMIN` não transaciona

Um usuário com perfil `ADMIN` recebe `403` ao tentar `POST /transacoes/efetuar`.

**Por quê:** segregação de funções. Quem administra o sistema — cria usuários, audita transações de qualquer conta — não deveria poder originar transações em nome de ninguém. Num contexto antifraude, acumular esses dois poderes no mesmo perfil cria justamente o vetor de fraude interna que o sistema existe para detectar.

**Custo aceito:** testar o fluxo de transação exige um segundo usuário. É atrito de desenvolvimento em troca de uma fronteira de privilégio explícita.

### 3. O evento é publicado dentro da transação

`save` e `convertAndSend` compartilham o mesmo `@Transactional`.

**Por quê:** garante que nunca existe transação persistida sem evento correspondente. A alternativa — publicar depois do commit — trocaria esse problema por transações órfãs, invisíveis ao motor de risco.

**Custo aceito:** existe uma janela de *dual write*. O canal AMQP não é transacional, então se a publicação tiver sucesso e o commit falhar em seguida, um evento sai para uma transação que não existe no banco. A solução correta é o **Outbox Pattern** — gravar o evento numa tabela na mesma transação e publicar via processo separado. Está fora do escopo desta versão, e a escolha atual privilegia o cenário de falha mais provável (broker indisponível) sobre o mais raro (falha no commit após publicação bem-sucedida).

### 4. Autoridade de authorization no banco, não no token

O JWT carrega um claim `perfil`, mas ele **nunca é lido** na validação. As authorities são recarregadas do banco a cada requisição, via `UsuarioDetailsService`.

**Por quê:** revogação de privilégio tem efeito imediato. Se um `ADMIN` for rebaixado, o token antigo continua válido mas já não carrega poder administrativo. Confiar no claim significaria esperar até 24 h pela expiração.

**Custo aceito:** um `SELECT` por requisição autenticada. Em escala, isso pede cache — mas trocar correção por latência antes de ter o problema de latência seria otimização prematura.

### 5. Schema gerado pelo Hibernate

`ddl-auto=update` em vez de Flyway ou Liquibase.

**Por quê:** velocidade de iteração em projeto de aprendizado, com o schema ainda em movimento.

**Custo aceito:** é a decisão menos defensável da lista para produção. `update` não remove colunas, não versiona, e não é reproduzível. Migrations são o próximo passo natural deste repositório.

---

## Testes

```bash
./mvnw verify
```

`verify` é o comando correto — e é o mesmo que a CI executa. `mvnw test` roda apenas os testes unitários: os de integração seguem o sufixo `*IT` e são executados pelo `maven-failsafe-plugin` na fase `integration-test`, o que garante o teardown dos containers mesmo quando um teste falha.

**Testes unitários** (`Mockito`): 22 testes cobrindo as regras de negócio dos três services em isolamento, incluindo cada caminho de negação de acesso.

**Testes de integração** (`Testcontainers`): sobem PostgreSQL 16 e RabbitMQ 3.13 reais em Docker. Nada é mockado.

- `TransacaoFluxoCompletoIT` — ponta a ponta: registro → login → transação → verificação da linha persistida → **consumo da mensagem real da fila** (via Awaitility) → consulta. É o teste que prova que a integração assíncrona funciona, não que ela foi chamada.
- `TransacaoSegurancaIT` — 8 cenários de rejeição: `401` sem token, `403` para outra conta, `404`, `400` em país e valor inválidos, `409` em e-mail duplicado.

A classe base compartilha uma única instância de cada container entre todas as classes de teste, via campos `static` com `@ServiceConnection`:

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegracaoBaseTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static { POSTGRES.start(); RABBITMQ.start(); }
}
```

A ausência de `@Testcontainers`/`@Container` é deliberada: aquela extension gerencia o ciclo de vida **por classe de teste**, parando os containers no `afterAll`. Como o Spring cacheia o `ApplicationContext` entre as classes, o pool de conexões manteria a porta da primeira execução enquanto o container era recriado em outra — falha que só aparece a partir da segunda classe de teste.

### CI

GitHub Actions em todo push e PR para `develop` e `main`: Java 21 (Temurin), cache Maven, `mvn -B verify` com Testcontainers usando o Docker do runner, e publicação dos relatórios de teste como artifact.

---

## Limitações conhecidas

Explícitas, porque um serviço financeiro merece honestidade sobre o que ainda não faz:

- **Confiabilidade de mensageria** — sem publisher confirms, retry ou dead-letter queue. Uma falha de broker resulta em `500` e rollback; não há reprocessamento automático.
- **Sem migrations** — schema gerenciado por `ddl-auto=update` (ver decisão 5).
- **Paginação sem ordenação** — `PageRequest` sem `Sort`, o que torna a ordem entre páginas não determinística no PostgreSQL.
- **Sem observabilidade** — nenhum Actuator, métrica ou tracing distribuído. Num sistema de microsserviços, correlation ID entre serviços é requisito, não luxo.
- **Sem rate limiting nem CORS** — o serviço é o único ponto de entrada público do sistema, o que torna ambos relevantes.
- **`ADMIN` sem cobertura end-to-end** — os endpoints `/admin/**` têm testes unitários de service, mas nenhum IT autentica como administrador.
- **Sem refresh token** — expirado o JWT de 24 h, o fluxo é novo login.

---

## Stack

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | LTS |
| Spring Boot | 3.5.14 | Framework base |
| Spring Web | — | API REST |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA | 3.5.x | Persistência |
| Spring AMQP | — | Integração RabbitMQ |
| Bean Validation | — | Validação declarativa nos DTOs |
| PostgreSQL | 16 | Banco relacional |
| RabbitMQ | 3.13 | Broker de mensagens |
| jjwt | 0.13.0 | Geração e validação de JWT |
| SpringDoc OpenAPI | 2.8.16 | Swagger UI |
| Lombok | 1.18.46 | Boilerplate |
| JUnit 5 + Mockito | — | Testes unitários |
| Testcontainers | 1.20.4 | Testes de integração |
| Awaitility | — | Asserções sobre fluxo assíncrono |
| Maven Failsafe | — | Separação unit / integração |
| Docker + Compose | — | Containerização |

`BigDecimal` para valores monetários, `UUID` como identificador (não sequencial, não enumerável) e `record` para todos os DTOs.

---

## Configuração

Todas as variáveis têm default para desenvolvimento local. Em qualquer ambiente compartilhado, `JWT_SECRET` e `ADMIN_SENHA` **precisam** ser sobrescritas — os defaults estão neste repositório público.

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/antifraude` | URL do PostgreSQL |
| `DB_USERNAME` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `RABBITMQ_HOST` | `localhost` | Host do broker |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_USERNAME` | `guest` | Usuário do broker |
| `RABBITMQ_PASSWORD` | `guest` | Senha do broker |
| `JWT_SECRET` | *(chave local)* | Segredo HMAC — mínimo 256 bits |
| `JWT_EXPIRACAO` | `86400000` | Validade do token em **milissegundos** |
| `ADMIN_EMAIL` | `admin@antifraude.com` | E-mail do ADMIN inicial |
| `ADMIN_SENHA` | `admin123456` | Senha do ADMIN inicial |
| `ADMIN_NOME` | `Administrador` | Nome do ADMIN inicial |

---

## Estrutura do projeto

```
src/main/java/antifraud/servicotransacao/
├── config/           # SecurityConfig, RabbitMQConfig, OpenApiConfig, DadosAdminInicial
├── controller/       # AuthUsuario, AdminUsuario, Transacao
├── dto/
│   ├── erros/        # ErroResponseDTO, ErroValidacaoResponseDTO
│   ├── transacao/    # request, response e evento publicado na fila
│   └── usuario/      # login/ e registro/
├── entity/           # Transacao, Usuario (implements UserDetails)
├── enums/            # StatusTransacao, PerfilUsuario
├── exception/        # 5 exceções de domínio + GlobalExceptionHandler
├── messaging/        # TransacaoPublisher
├── repository/       # Spring Data JPA
├── security/         # JwtService, JwtAuthFilter, UsuarioDetailsService, handlers 401/403
├── service/          # regras de negócio
└── util/             # mappers e envelope de paginação

src/test/java/antifraud/servicotransacao/
├── integration/      # IntegracaoBaseTest + 2 ITs com Testcontainers
└── service/          # 3 suítes unitárias com Mockito
```

Cada camada tem uma responsabilidade e uma direção de dependência: `controller` conhece `service`, `service` conhece `repository` e `messaging`, e nenhuma delas conhece a de cima.

---

## Parte de um sistema maior

| Repositório | Papel |
|---|---|
| [antifraud-system](https://github.com/LS-PLuka/antifraud-system) | Orquestração e documentação geral |
| **servico-transacao** | **Este repositório** — entrada e registro de transações |
| [motor-risco](https://github.com/LS-PLuka/motor-risco) | Análise de risco com cadeia de regras |
| [servico-auditoria](https://github.com/LS-PLuka/servico-auditoria) | Histórico de decisões |

---

Desenvolvido por [Pedro Luka](https://github.com/LS-PLuka).
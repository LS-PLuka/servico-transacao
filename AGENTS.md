# AGENTS.md

## Escopo

Estas instrucoes valem para todo o repositorio. Antes de alterar qualquer arquivo, leia este documento e inspecione o codigo relacionado, os testes existentes e a configuracao afetada.

## Principio fundamental

Preserve o padrao de codigo existente. Antes de criar ou modificar uma classe, procure classes equivalentes no projeto e use-as como referencia para nomenclatura, estrutura, anotacoes, injecao de dependencias, logs, tratamento de excecoes e testes. Nao introduza uma arquitetura, biblioteca ou estilo novo sem uma necessidade clara e autorizacao do responsavel pelo repositorio.

## Arquitetura e limites das camadas

- A aplicacao usa Java 21, Spring Boot, Spring Web, Spring Security, Spring Data JPA, Spring AMQP, PostgreSQL e RabbitMQ.
- Mantenha o pacote raiz `antifraud.servicotransacao` e a organizacao atual por camada: `config`, `controller`, `dto`, `entity`, `enums`, `exception`, `messaging`, `repository`, `security`, `service` e `util`.
- Preserve a direcao das dependencias: controller chama service; service concentra regras de negocio e chama repository, publisher e utilitarios; repository trata persistencia; publisher encapsula RabbitMQ; mapper converte entre entidade e DTO.
- Controllers devem permanecer finos: validar entrada com Bean Validation, registrar a tentativa em log, delegar ao service e montar o `ResponseEntity`.
- Nao exponha entidades JPA diretamente na API ou na mensageria. Use DTOs e mappers.
- DTOs sao `record` e devem permanecer imutaveis. Coloque validacoes declarativas e documentacao OpenAPI nos DTOs de entrada e saida quando aplicavel.
- Entidades usam JPA e Lombok, com `UUID` para identificadores, `BigDecimal` para valores monetarios e `LocalDateTime` para datas.
- Repositories devem estender `JpaRepository` e preferir query methods do Spring Data quando forem suficientes.
- Services usam `@Service`, `@RequiredArgsConstructor`, campos `private final` e injecao por construtor. Use `@Transactional` em operacoes de escrita e `@Transactional(readOnly = true)` em consultas.
- Nao coloque regra de negocio em controllers, repositories, publishers ou mappers.

## Convencoes de codigo

- Use nomes em portugues, coerentes com o dominio e com as classes vizinhas.
- Siga a indentacao atual de quatro espacos, uma classe publica por arquivo e imports organizados como nos arquivos equivalentes.
- Prefira os padroes ja usados: builders para montar entidades, records para DTOs, metodos estaticos em mappers e retornos com `ResponseEntity` nos controllers.
- Mantenha metodos pequenos e com fluxo legivel. Extraia auxiliares somente quando isso melhorar a clareza ou eliminar duplicacao relevante.
- Use `@Slf4j` e placeholders `{}`. Nao registre senhas, tokens, segredos ou outros dados sensiveis.
- Preserve o idioma e o formato das mensagens de log, validacao e excecao, salvo quando a tarefa exigir uma mudanca de contrato.
- Comentarios devem explicar decisoes ou regras nao obvias; nao descreva mecanicamente o que o codigo ja mostra.
- Evite imports, campos, metodos e DTOs sem uso. Ao encontrar codigo preexistente aparentemente sem uso, nao o remova fora do escopo da tarefa.

## Excecoes e contratos HTTP

- Reutilize as excecoes de dominio existentes antes de criar uma nova.
- Novas excecoes de API devem ser tratadas centralmente em `GlobalExceptionHandler`, usando `ErroResponseDTO` ou `ErroValidacaoResponseDTO` e status HTTP coerente.
- Falhas de autenticacao e autorizacao devem continuar integradas ao Spring Security e aos handlers existentes.
- Mudancas em DTOs, status HTTP, mensagens, nomes de campos JSON, rotas ou eventos RabbitMQ sao mudancas de contrato. Analise consumidores e testes antes de faze-las e documente o impacto.

## Persistencia e mensageria

- A relacao vigente e que `Transacao.contaId` corresponde ao `Usuario.id`; nao suponha uma entidade `Conta` inexistente.
- Uma nova transacao nasce com status `PENDENTE`.
- A topologia RabbitMQ atual e: exchange direta `transacoes.exchange`, routing key `transacoes.risco` e fila duravel `transacoes.analise`.
- Centralize nomes da topologia em `RabbitMQConfig` e publique por meio de `TransacaoPublisher` com conversao JSON.
- Trate alteracoes em `TransacaoEventoDTO` como versionamento de contrato com o motor de risco. Verifique serializacao e teste o payload publicado.
- A persistencia e a publicacao atuais formam um dual write dentro do metodo transacional. Nao presuma atomicidade entre PostgreSQL e RabbitMQ; qualquer mudanca nessa estrategia exige analise explicita de falhas, duplicidade, idempotencia e consistencia.
- Nao altere `ddl-auto`, schema, precisao monetaria ou colunas sem avaliar migracao e compatibilidade dos dados existentes.

## Seguranca

- Preserve a autenticacao stateless por JWT e as regras atuais: rotas `/auth/**` especificadas sao publicas, `/admin/**` exige `ADMIN` e as demais exigem autenticacao.
- O usuario autenticado e carregado do banco e disponibilizado no `SecurityContext`; valide perfil e posse no service conforme os exemplos existentes.
- Usuario `ADMIN` nao pode efetuar transacoes, mas pode consultar transacoes de outras contas.
- Nunca enfraqueca validacoes de posse, perfil, token ou credenciais para simplificar uma implementacao ou teste.
- Segredos e credenciais devem vir de propriedades/variaveis de ambiente. Nao adicione credenciais reais ao repositorio.

## Testes obrigatorios

- Toda classe com logica de negocio, especialmente services, deve ter testes unitarios.
- Antes de implementar, leia a suite equivalente em `src/test/java/antifraud/servicotransacao/service` e siga JUnit 5, Mockito, `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `@BeforeEach`, `@DisplayName` e o padrao de nome `metodo_cenario_resultado`.
- Cubra caminho feliz, validacoes, negacoes de acesso, excecoes, interacoes esperadas e a ausencia de chamadas quando o fluxo deve ser interrompido.
- Ao modificar um metodo ja testado, execute os testes correspondentes e ajuste-os somente quando a mudanca de comportamento for intencional. Preserve o comportamento esperado fora do escopo solicitado.
- Para mudancas no fluxo HTTP, persistencia real, seguranca ou RabbitMQ, adicione ou ajuste testes de integracao `*IT` baseados em `IntegracaoBaseTest`, PostgreSQL e RabbitMQ via Testcontainers.
- Nao substitua uma verificacao de integracao real da fila apenas por mock do publisher quando o contrato de mensageria for alterado.
- Use `./mvnw test` (ou `mvnw.cmd test` no Windows) para testes unitarios e `./mvnw verify` (ou `mvnw.cmd verify`) para a suite completa, incluindo Failsafe/Testcontainers.
- Informe quais comandos foram executados e qualquer teste nao executado, com o motivo.

## Fluxo de trabalho para alteracoes

1. Verifique `git status` e preserve alteracoes preexistentes do usuario.
2. Localize classes e testes equivalentes antes de editar.
3. Rastreie o fluxo completo afetado, incluindo contratos HTTP, banco, mensageria e seguranca.
4. Implemente apenas o escopo solicitado, sem refatoracoes oportunistas.
5. Crie ou ajuste primeiro os testes necessarios para demonstrar o comportamento esperado.
6. Execute testes proporcionais ao risco; para mudancas transversais, execute `verify`.
7. Revise o diff para garantir que nao houve mudanca acidental de contrato, formatacao ou comportamento.
8. Resuma arquivos alterados, decisoes, testes e riscos residuais ao entregar.

## Documentacao

- Atualize README e OpenAPI quando uma mudanca alterar rotas, payloads, configuracao, regras de acesso, fluxo operacional ou contratos RabbitMQ.
- Mantenha documentacao e codigo sincronizados. Se identificar divergencia fora do escopo, registre-a na entrega em vez de corrigi-la silenciosamente.

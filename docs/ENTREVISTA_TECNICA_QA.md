# Perguntas e Respostas — Entrevista Técnica (Desafio Coupon API)

Documento com perguntas e respostas detalhadas sobre o projeto **Coupon API**, pensado para preparação ou condução de entrevista técnica.

---

## 1. Visão geral e contexto do desafio

### 1.1 Qual é o objetivo principal desta API?

A API gerencia o **ciclo de vida de cupons de desconto**, com foco em **criar** e **deletar** (soft delete). O desafio não é um CRUD genérico: o peso está em fazer Create e Delete respeitando todas as regras de negócio (por exemplo, não permitir deletar o mesmo cupom duas vezes) e em ter um design que permita testar **comportamento**, não só tecnologia.

### 1.2 Por que o foco em Create e Delete e não em um CRUD completo?

O enunciado deixa claro que o importante é entregar um projeto **funcional, organizado, com regras bem definidas e testáveis**. A ideia é que a avaliação tente “quebrar” as regras; por isso, mocks sozinhos não bastam — é necessário validar o que pode ou não acontecer no sistema. Create e Delete concentram as regras mais sensíveis (validação na criação, proteção contra deleção dupla).

---

## 2. Arquitetura

### 2.1 Que estilo de arquitetura foi adotado? Por quê?

Foi adotada **arquitetura hexagonal** (portas e adaptadores) aliada a ideias de **clean architecture**: separação em **domain**, **application** e **infrastructure**.

- **Domain**: núcleo do negócio, sem dependências de framework ou banco.
- **Application**: casos de uso que orquestram o fluxo e dependem apenas de **interfaces** (portas).
- **Infrastructure**: adaptadores que implementam as portas (JPA, HTTP, etc.).

Isso permite testar regras de negócio sem Spring/JPA, trocar persistência ou canal de entrada (REST, fila, CLI) sem alterar o núcleo, e manter a aplicação agnóstica à tecnologia.

### 2.2 O que são “portas” e “adaptadores” neste projeto?

- **Portas**: interfaces definidas na camada de application que representam o que o sistema precisa do mundo externo (ex.: salvar e carregar cupom). Exemplos: `SaveCouponPort`, `LoadCouponPort`. A application **só** depende dessas interfaces.

- **Adaptadores**: implementações concretas na infrastructure. O `CouponPersistenceAdapter` implementa as duas portas usando Spring Data JPA e `CouponEntity`. O controller é o adaptador HTTP que chama os use cases e devolve DTOs.

### 2.3 Por que a camada de application não deve importar JPA ou Spring Web?

Para respeitar **inversão de dependência**: a aplicação define “o que precisa” (portas), e a infraestrutura fornece “como fazer”. Se a application importasse `javax.persistence` ou `org.springframework.web`, ela ficaria acoplada a uma tecnologia específica. Assim, é possível rodar os mesmos use cases em outro contexto (CLI, fila, outro framework) sem mudar a lógica de aplicação. O “porteiro” (application) não precisa saber o que tem lá fora (JPA, REST, etc.).

### 2.4 Onde os use cases são instanciados? Por que não usar @Service na application?

Os use cases são registrados como beans em **`UseCaseConfig`**, dentro de **infrastructure/config**. Assim, a camada de application permanece sem anotações Spring; a “montagem” da aplicação fica na infra. O Spring injeta as portas (implementadas pelo adapter) nos construtores dos use cases (via `@RequiredArgsConstructor`).

---

## 3. Domínio (Domain)

### 3.1 O que é o modelo de domínio Coupon e por que ele não é a entidade JPA?

O **Coupon** é o **modelo rico de domínio**: contém dados do cupom e **todas as regras de negócio** (criação, validações, soft delete, “não deletar duas vezes”). Ele não tem anotações JPA; é uma classe pura de domínio.

A **CouponEntity** fica na infrastructure e é o modelo de persistência (tabela `coupons`). O adapter converte `Coupon` ↔ `CouponEntity`. Essa separação garante que o domínio não dependa de banco de dados e que as regras fiquem em um único lugar, testável sem JPA.

### 3.2 Quais regras de negócio estão no domínio e onde exatamente?

- **Criação** (`Coupon.create(...)`):
  - Código obrigatório, normalizado: exatamente 6 caracteres alfanuméricos, maiúsculas (remove caracteres especiais).
  - Desconto obrigatório e mínimo 0,5.
  - Data de expiração obrigatória, formato `yyyy-MM-dd`, e **obrigatoriamente futura**.
  - Status inicial `ACTIVE`.
- **Soft delete** (`Coupon.delete()`): altera o status para `DELETED`.
- **Proteção contra deleção dupla**: se o status já for `DELETED`, `delete()` lança `IllegalStateException`.

Todas essas validações e alterações de estado estão em métodos do próprio `Coupon` (ou estáticos privados usados por `create`), não no use case nem no controller.

### 3.3 O que é o método `reconstitute` e por que existe?

`Coupon.reconstitute(...)` é um **factory method** que monta um `Coupon` a partir de dados já persistidos (id, code, description, etc.), **sem** rodar as validações de criação. É usado pelo **CouponPersistenceAdapter** ao converter `CouponEntity` → `Coupon` após ler do banco. Assim, o domínio controla a única forma de “recriar” um agregado a partir do repositório, sem expor construtor público com muitos parâmetros.

### 3.4 Por que BusinessException não tem @ResponseStatus?

Para manter o **domain** livre de conceitos de HTTP. A exceção representa “regra de negócio violada”; quem decide que isso vira status 400 e corpo JSON é a **infrastructure** (GlobalExceptionHandler). Assim, o domínio continua utilizável em outros contextos (CLI, outro tipo de API).

---

## 4. Camada de aplicação (Use cases e portas)

### 4.1 O que é um “use case” neste projeto e quantos métodos públicos ele tem?

Cada use case é uma classe com **uma única intenção** e, em geral, **um único método público** (`execute`). Exemplos: `CreateCouponUseCase`, `DeleteCouponUseCase`, `GetCouponUseCase`. Eles **orquestram** o fluxo (chamam o domínio e as portas), mas não implementam regras de negócio; as regras ficam no `Coupon`.

### 4.2 Por que evitar um “CustomerService” com muitos métodos?

Um service genérico tende a acumular muitas responsabilidades (criar, editar, deletar, notificar, relatório, etc.), o que dificulta testes, entendimento e manutenção. No projeto, cada **intenção do usuário** vira um use case com um `execute` claro. Isso segue o princípio de **uma classe, uma razão para mudar** e facilita testes focados (ex.: só o fluxo de deletar).

### 4.3 No DeleteCouponUseCase, por que capturar IllegalStateException e lançar BusinessException?

`Coupon.delete()` lança `IllegalStateException` quando o cupom já está deletado (regra de domínio). O use case traduz isso para `BusinessException` para que a camada de infra (GlobalExceptionHandler) trate de forma uniforme e devolva 400 com mensagem amigável em JSON. Assim, a API expõe uma mensagem de negócio consistente em vez do detalhe técnico da exceção do domínio.

### 4.4 As portas retornam e recebem tipos de qual camada?

As portas trabalham com tipos do **domain** (`Coupon`, `Optional<Coupon>`, `UUID`). A application não conhece DTOs de API nem entidades JPA; o controller faz a tradução DTO → parâmetros do use case e Coupon → DTO de resposta.

---

## 5. Infraestrutura

### 5.1 Qual o papel do CouponPersistenceAdapter?

Ele **implementa** `SaveCouponPort` e `LoadCouponPort` usando o `CouponRepository` (Spring Data JPA). Responsabilidades:

- **save(Coupon)**: converte `Coupon` → `CouponEntity`, chama `repository.save`, converte a entidade salva de volta para `Coupon` (com ID preenchido) e retorna.
- **findById(UUID)**: busca a entidade, converte para `Coupon` com `Coupon.reconstitute(...)` e retorna `Optional<Coupon>`.

Assim, a application nunca vê JPA nem `CouponEntity`.

### 5.2 Por que existem dois “modelos” de cupom (Coupon e CouponEntity)?

- **Coupon**: modelo de domínio, com regras e sem dependências de persistência. Usado em toda a application e no adapter ao falar com a application.
- **CouponEntity**: modelo de persistência (JPA), espelho da tabela. Usado só dentro da infrastructure. O adapter faz a ponte entre os dois.

Isso permite evoluir o esquema do banco ou o modelo de domínio com menos impacto, e testar o domínio sem banco.

### 5.3 Como os erros de validação e de regra de negócio chegam ao cliente em JSON?

O **GlobalExceptionHandler** (`@RestControllerAdvice`) com **`@Order(Ordered.HIGHEST_PRECEDENCE)`** trata as exceções antes do handler padrão do Spring. Para `MethodArgumentNotValidException` (Bean Validation), monta um body com `timestamp`, `status`, `error`, `errors` (lista de campo + mensagem) e `message` (texto concatenado). Para `BusinessException` e `ResourceNotFoundException`, também devolve JSON padronizado com `Content-Type: application/json`. Assim, o cliente sempre recebe o erro no body, em formato consistente.

### 5.4 Como a API aceita mais de um formato de data (ex.: 31-12-2026)?

Foi criado um **deserializador Jackson** (`ExpirationDateDeserializer`) no DTO de request. Ele aceita `yyyy-MM-dd` e `dd-MM-yyyy`, parseia, normaliza para `yyyy-MM-dd` e devolve a string. O restante do fluxo (Bean Validation, domínio) continua recebendo sempre o formato normalizado. A decisão de aceitar múltiplos formatos fica na borda (web), sem poluir o domínio.

---

## 6. Regras de negócio e validação

### 6.1 Onde fica a validação “data de expiração não pode ser anterior ao momento atual”?

Em dois níveis, com papéis diferentes:

1. **DTO (Bean Validation)**: o getter `isDataFutura()` no `CouponRequestDto` com `@AssertTrue` usa o campo `expirationDate` já normalizado pelo deserializador. Falha na validação do request quando a data não é futura.
2. **Domínio**: `Coupon.create(...)` chama `toFutureExpiration(expirationDate)`, que também exige data futura e lança `BusinessException` se não for. Garante a regra mesmo que o fluxo não passe pelo DTO (ex.: outro ponto de entrada no futuro).

### 6.2 Por que “não deletar duas vezes” está no domínio e não só no use case?

A regra “cupom já deletado não pode ser deletado de novo” é **regra de negócio** e deve estar no **agregado** (Coupon). O use case apenas chama `coupon.delete()`; quem decide se a operação é válida é o próprio `Coupon`. Isso centraliza a regra em um único lugar, facilita testes de domínio e evita duplicar a lógica em outros use cases ou adapters.

### 6.3 O que acontece se alguém tentar deletar o mesmo cupom duas vezes?

1. Primeira vez: `DeleteCouponUseCase.execute(id)` carrega o cupom, chama `coupon.delete()` (status vira `DELETED`), salva e retorna.
2. Segunda vez: ao chamar `coupon.delete()` de novo, o domínio lança `IllegalStateException`. O use case captura e lança `BusinessException`. O GlobalExceptionHandler retorna 400 com JSON contendo a mensagem “Não é possível deletar um cupom que já está deletado.” (e há teste de integração cobrindo esse cenário).

---

## 7. Testes

### 7.1 Como os testes estão organizados por camada?

- **Domain**: `CouponTest` — testes unitários do `Coupon` (create com validações, reconstitute, delete, “não deletar duas vezes”).
- **Application**: testes dos use cases (ex.: `CreateCouponUseCaseTest`, `DeleteCouponUseCaseTest`, `GetCouponUseCaseTest`) com portas mockadas; validam orquestração e comportamento (ex.: não persistir quando o domínio falha, não permitir deletar duas vezes).
- **Web**: `CouponControllerTest` — controller com use cases e mapper mockados; foco em HTTP (status, body).
- **Integração**: `CouponJourneyIT` — fluxo completo contra a API e o banco (criar → buscar → deletar) e cenário explícito de “deletar duas vezes” retornando 400.

### 7.2 Por que “mocks sozinhos não bastam”?

O enunciado diz que a avaliação vai tentar “quebrar” as regras. Testes só com mocks podem passar mesmo quando a regra real está errada (ex.: mock sempre retorna cupom ativo). Por isso há **testes de domínio** (regras reais no `Coupon`) e **testes de integração** (API + banco) que garantem que criar, buscar e deletar (incluindo “deletar duas vezes”) se comportam como esperado de ponta a ponta.

### 7.3 Como é gerado o relatório de cobertura (JaCoCo)?

Comando: `./gradlew test jacocoTestReport`. O relatório HTML é gerado em **`build/reports/jacoco/index.html`**. O JaCoCo está configurado no `build.gradle` (plugin jacoco, `test` finalizado por `jacocoTestReport`).

---

## 8. Tecnologias e ferramentas

### 8.1 Onde o Lombok é usado e com qual objetivo?

- **Domain**: `Coupon` com `@Getter` e `@AllArgsConstructor(access = AccessLevel.PRIVATE)` para evitar getters e construtor repetitivos; mantidos apenas `setId` e os métodos de domínio.
- **Use cases**: `@RequiredArgsConstructor` para gerar o construtor com os campos `final` (portas), usados na injeção pelo `UseCaseConfig`.
- **DTOs e CouponEntity**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` para getters, setters e construtores.

Objetivo: reduzir boilerplate mantendo o código legível e o design (imutabilidade onde faz sentido, exposição mínima).

### 8.2 Por que Java 21 e Spring Boot 3.x?

Java 21 é LTS e traz melhorias de performance e linguagem; Spring Boot 3.x usa Jakarta EE e é a linha atual recomendada para novos projetos. O projeto segue essas escolhas para alinhamento com o ecossistema e suporte de longo prazo.

### 8.3 O projeto usa MapStruct?

Não. O mapeamento entre `Coupon` e `CouponResponseDto` é feito manualmente no **CouponWebMapper** (infrastructure/web). O mapeamento entre `Coupon` e `CouponEntity` está no **CouponPersistenceAdapter**. Para o tamanho atual do modelo, mapeamento manual é suficiente e evita mais uma dependência e configuração.

---

## 9. Decisões de design e trade-offs

### 9.1 Por que soft delete em vez de delete físico?

Soft delete (mudar status para `DELETED`) preserva histórico e auditoria, permite “recuperar” ou consultar cupons antigos e evita quebrar referências em outros sistemas. O desafio especifica exclusão lógica; a modelagem com `CouponStatus.DELETED` reflete isso.

### 9.2 Por que o controller não recebe nem retorna objetos de domínio diretamente?

O controller é parte da **infrastructure** (adaptador HTTP). Ele deve falar em termos de contrato da API (DTOs), não expor o modelo de domínio. Assim, a API pode evoluir (renomear campos, versionar) sem alterar o domínio, e o domínio permanece independente de detalhes de serialização JSON.

### 9.3 Se amanhã a entrada fosse uma fila (ex.: RabbitMQ) em vez de REST, o que mudaria?

Seria criado um **novo adaptador** (consumer da fila) que deserializa a mensagem, chama os mesmos use cases (`createCouponUseCase.execute(...)`, etc.) e publica o resultado ou erro em outra fila/tópico. O **domain** e a **application** (portas e use cases) permaneceriam iguais; apenas a infrastructure ganharia um novo adaptador. Isso ilustra a aplicação “agnóstica” do canal de entrada.

---

## 10. Extensibilidade e manutenção

### 10.1 Como adicionar uma nova regra de negócio na criação do cupom?

A regra deve ser implementada no **domínio**, em `Coupon.create(...)` (ou em um método estático privado chamado por `create`). Exemplo: “código não pode ser uma palavra bloqueada” — novo método `validateCodeNotBlocked(code)` chamado dentro de `create`, lançando `BusinessException` se violado. Os testes de `CouponTest` devem ser ampliados para cobrir o novo comportamento; os use cases e a API não precisam mudar a estrutura, apenas repassar a nova validação.

### 10.2 Como adicionar um novo caso de uso (ex.: “listar cupons ativos”)?

1. Definir a porta necessária na application (ex.: `FindActiveCouponsPort` retornando `List<Coupon>`).
2. Criar o use case (ex.: `ListActiveCouponsUseCase`) com um único `execute()` e dependendo apenas da porta.
3. No **CouponPersistenceAdapter** (ou em um repositório específico), implementar a porta (ex.: query por `status = ACTIVE`).
4. Registrar o use case em **UseCaseConfig** e expor no controller (ex.: `GET /coupons?status=ACTIVE`).
Domain e regras de “o que é ativo” podem ficar no domínio se fizer sentido (ex.: método `Coupon.isActive()`).

### 10.3 Onde documentar a API para quem for consumir?

Além do README (endpoints, exemplos de payload e resposta de erro), pode-se usar **SpringDoc OpenAPI (Swagger)**:
- Configurar a dependência e acessar `http://localhost:8080/swagger-ui/index.html` com a aplicação rodando.
- Os DTOs e o controller já podem ser anotados com `@Schema` e descrições para enriquecer a documentação gerada.

---

## Resumo rápido para o candidato

- **Arquitetura**: hexagonal / clean (domain, application com portas, infrastructure com adapters).
- **Foco**: Create e Delete com regras bem definidas e testáveis.
- **Domain**: modelo `Coupon` com todas as regras; sem JPA/Spring.
- **Application**: use cases com um `execute`; dependem só de portas (interfaces).
- **Infrastructure**: adapter JPA implementa portas; controller e GlobalExceptionHandler tratam HTTP e erros em JSON.
- **Testes**: unitários no domínio e nos use cases; integração (API + banco) para fluxos e “quebrar” regras.
- **Ferramentas**: Lombok para boilerplate; JaCoCo para cobertura (`./gradlew test jacocoTestReport`).

Este documento cobre o que seria esperado em uma entrevista técnica bem detalhista sobre o desafio desenvolvido.

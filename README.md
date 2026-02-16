# Coupon API

API REST em **Java 21** e **Spring Boot** para gerenciamento do ciclo de vida de cupons de desconto. O projeto segue **arquitetura hexagonal / clean architecture**, com foco em **Create** e **Delete**, regras de negócio no domínio e camada de aplicação agnóstica a tecnologia.

## Tecnologias

### Core & Framework
* **Java 21 (LTS)**
* **Spring Boot 3.2.x**
* **Gradle** (build)

### Banco de Dados
* **H2** (em memória)
* **Spring Data JPA** (apenas na camada de infraestrutura)

### Qualidade e produtividade
* **Lombok** — redução de boilerplate: `@Getter`, `@RequiredArgsConstructor`, `@Data`, `@AllArgsConstructor` em domain, use cases, DTOs e entidades JPA.
* **JaCoCo** — relatório de cobertura de testes (HTML em `build/reports/jacoco/`).
* **SpringDoc OpenAPI (Swagger)**
* **Docker & Docker Compose**

### Testes
* **JUnit 5**, **Mockito**, **Spring Boot Test**

---

## Arquitetura

O projeto está organizado em **domain**, **application** e **infrastructure**, com inversão de dependência: a camada de aplicação não conhece JPA nem Spring Web.

### Estrutura de pacotes

```
com.coupon.demo/
├── CouponApplication.java
│
├── domain/                         # Núcleo do negócio (sem Spring, sem JPA)
│   ├── Coupon.java                 # Modelo rico: create(), delete(), reconstitute()
│   ├── CouponStatus.java           # ACTIVE, INACTIVE, DELETED
│   └── BusinessException.java     # Exceção de regra (sem @ResponseStatus)
│
├── application/                    # Casos de uso e portas (sem infra)
│   ├── port/
│   │   ├── SaveCouponPort.java
│   │   └── LoadCouponPort.java
│   ├── usecase/
│   │   ├── CreateCouponUseCase.java   # execute(...)
│   │   ├── DeleteCouponUseCase.java   # execute(UUID)
│   │   └── GetCouponUseCase.java      # execute(UUID)
│   └── exception/
│       └── ResourceNotFoundException.java
│
├── infrastructure/                 # Adaptadores (JPA, HTTP)
│   ├── config/
│   │   └── UseCaseConfig.java           # Beans dos use cases
│   ├── persistence/
│   │   ├── CouponEntity.java            # Modelo JPA (sem regras)
│   │   ├── CouponRepository.java        # Spring Data JPA
│   │   └── CouponPersistenceAdapter.java # Implementa SaveCouponPort, LoadCouponPort
│   └── web/
│       ├── CouponController.java        # HTTP → use cases
│       ├── CouponWebMapper.java         # Coupon → CouponResponseDto
│       └── GlobalExceptionHandler.java # Exceções → JSON no body (400, 404, etc.)
│
└── dto/
    ├── request/
    │   ├── CouponRequestDto.java        # Entrada da API (Bean Validation + isDataFutura)
    │   └── ExpirationDateDeserializer.java  # Aceita yyyy-MM-dd ou dd-MM-yyyy
    └── response/
        └── CouponResponseDto.java
```

### Princípios aplicados

| Princípio | Como |
|-----------|------|
| **Um use case, uma intenção** | Cada classe em `application.usecase` tem um único método público (`execute`) e uma responsabilidade clara. Construtores via `@RequiredArgsConstructor`. |
| **Inversão de dependência** | A application depende apenas de **interfaces** (`SaveCouponPort`, `LoadCouponPort`). A implementação JPA fica em `CouponPersistenceAdapter`. |
| **Regras no domínio** | Validações (código 6 caracteres, desconto mínimo, data futura, “não deletar duas vezes”) ficam em `Coupon.create()` e `Coupon.delete()`. O use case só orquestra. |
| **Application agnóstica** | Em `domain` e `application` não há `import` de `org.springframework.*` nem `jakarta.persistence.*`. A tradução para HTTP fica em `infrastructure.web`. |
| **Boilerplate reduzido** | Domain: `@Getter` e `@AllArgsConstructor(access = PRIVATE)` em `Coupon`. Use cases: `@RequiredArgsConstructor`. DTOs e entidade JPA: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`. |

### Regras de negócio (domínio)

- **Criação** (`Coupon.create(...)`):
  - Código normalizado: 6 caracteres alfanuméricos, maiúsculas.
  - Desconto mínimo 0,5.
  - Data de expiração obrigatória, formato `yyyy-MM-dd` (após normalização), obrigatoriamente futura.
  - Status inicial `ACTIVE`.
- **Soft delete** (`Coupon.delete()`): altera status para `DELETED`.
- **Proteção contra deleção dupla**: se já estiver `DELETED`, `delete()` lança exceção e a API retorna `400 Bad Request` com mensagem no body.

### Fluxo (exemplo: criar cupom)

1. **Controller** recebe o DTO, valida (`@Valid`) e chama `createCouponUseCase.execute(...)`.
2. **CreateCouponUseCase** chama `Coupon.create(...)` (regras no domínio) e depois `saveCouponPort.save(coupon)`.
3. **CouponPersistenceAdapter** converte `Coupon` → `CouponEntity`, persiste via JPA e devolve `Coupon` com ID.
4. **Controller** usa `CouponWebMapper` para montar `CouponResponseDto` e retornar HTTP 201.

---

## Como rodar

### Pré-requisitos
* Docker Desktop **ou** JDK 21.

### Com Docker
```bash
docker compose up --build
```
API em: `http://localhost:8080`.

### Com Gradle
```powershell
./gradlew bootRun
```
(Linux/Mac: `./gradlew bootRun`)

---

## API

### Endpoints

| Verbo  | Endpoint       | Descrição |
|--------|----------------|-----------|
| POST   | `/coupon`      | Cria cupom (status ACTIVE). |
| GET    | `/coupon/{id}` | Busca cupom por ID. |
| DELETE | `/coupon/{id}` | Soft delete (status → DELETED). Não permite deletar duas vezes. |

Documentação (se Swagger estiver ativo): `http://localhost:8080/swagger-ui/index.html`

### Formato de entrada (POST /coupon)

- **expirationDate**: aceita **yyyy-MM-dd** (ex.: `2026-12-31`) ou **dd-MM-yyyy** (ex.: `31-12-2026`). O deserializador normaliza para uso interno.

Exemplo de payload:
```json
{
  "code": "AAA001",
  "description": "Cupom de teste",
  "discountValue": 0.5,
  "expirationDate": "31-12-2026",
  "published": true
}
```

### Resposta de erro (validação e regras)

Erros de validação (Bean Validation) e de regra de negócio retornam **JSON no body** com estrutura padronizada:

```json
{
  "timestamp": "2026-02-13T22:10:49.715",
  "status": 400,
  "error": "Bad Request",
  "errors": [
    { "field": "dataFutura", "message": "A data de expiração não pode ser anterior ao momento atual" }
  ],
  "message": "dataFutura: A data de expiração não pode ser anterior ao momento atual"
}
```

O `GlobalExceptionHandler` (`@RestControllerAdvice` com `@Order(HIGHEST_PRECEDENCE)`) garante que a resposta seja sempre em JSON com `Content-Type: application/json`.

---

## Testes e cobertura (JaCoCo)

- **Domain:** `CouponTest` — regras de criação e de “não deletar duas vezes”.
- **Application:** `CreateCouponUseCaseTest`, `DeleteCouponUseCaseTest`, `GetCouponUseCaseTest` — use cases com portas mockadas.
- **Web:** `CouponControllerTest` — controller com use cases e mapper mockados.
- **Integração:** `CouponJourneyIT` — fluxo completo (criar → buscar → deletar) e cenário “deletar duas vezes” retornando 400.

### Executar testes
```powershell
./gradlew test
```
(Linux/Mac: `./gradlew test`)

### Gerar relatório de cobertura (JaCoCo)
```powershell
./gradlew test jacocoTestReport
```
Relatório HTML: **`build/reports/jacoco/index.html`** (abrir no navegador).

(Linux/Mac: `./gradlew test jacocoTestReport`)

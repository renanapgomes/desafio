# Coupon API

Uma API RESTful robusta desenvolvida em **Java 21** e **Spring Boot** para o gerenciamento do ciclo de vida de cupões de desconto. O projeto implementa boas práticas de desenvolvimento, incluindo arquitetura em camadas, Soft Delete, validações automáticas e testes de integração.

## Tecnologias Utilizadas

### Core & Framework
* **Java 21 (LTS)**
* **Spring Boot 3.2.5**
* **Gradle** (Build Tool)

### Banco de Dados
* **H2 Database** (Em memória para execução rápida e leve)
* **Spring Data JPA** (Persistência)

### Ferramentas & Utilitários
* **Lombok:** Redução de boilerplate (Getters, Setters, Builders).
* **MapStruct:** Mapeamento simples entre Entidade e DTO de resposta.
* **SpringDoc OpenAPI (Swagger):** Documentação viva da API.
* **Docker & Docker Compose:** Containerização.

### Qualidade de Código (Testes)
* **JUnit 5**
* **Mockito**
* **Spring Boot Test** (Integração e Contexto)
* **JaCoCo** (relatório de cobertura de testes)


## Arquitetura e Design

O projeto segue uma arquitetura em camadas limpa:

1.  **Controller:** Camada de entrada, responsável por receber HTTP, validar DTOs (`@Valid`) e delegar para o serviço.
2.  **Service (Aplicação):** Orquestra o fluxo da requisição, conversa com o repositório e com o domínio. Não contém regra de negócio complexa.
3.  **Domínio (`CouponEntity`):** Onde vivem as principais regras de negócio (DDD). Define como um cupom é criado, quando pode ser deletado etc.
4.  **Repository:** Interface de comunicação com o banco de dados.
5.  **Mapper:** Camada de transformação simples de Entidade → DTO de resposta.

### Regras de negócio no domínio (DDD)

As regras mais importantes estão encapsuladas em métodos de domínio na `CouponEntity`:

- **Criação de cupom**: método estático `CouponEntity.create(...)` centraliza:
  - Normalização do código (`code`) removendo caracteres especiais, deixando em upper-case e garantindo exatamente 6 caracteres alfanuméricos.
  - Validação do desconto mínimo (`discountValue >= 0.5`).
  - Validação da data de expiração em formato `yyyy-MM-dd` e obrigatoriamente futura.
  - Definição do `status` inicial como `ACTIVE` e do flag `published`.
- **Soft Delete**: método `delete()` altera o `status` de `ACTIVE` para `DELETED` sem remover o registro do banco.
- **Proteção contra deleção dupla**: se o cupom já estiver `DELETED`, `delete()` lança erro de domínio e a API retorna `400 Bad Request` com mensagem explicativa.

## Como Rodar o Projeto

### Pré-requisitos
* **Docker Desktop** (Recomendado)
* OU Java 21 JDK instalado.

### Opção 1: Via Docker (Zero Configuração)
Esta é a maneira mais fácil, pois não requer Java instalado na máquina local.

1.  Na raiz do projeto, execute:
    ```bash
    docker compose up --build
    ```
2.  Aguarde a inicialização. A API estará disponível em: `http://localhost:8080`.

3.  Para parar a aplicação:
    ```bash
    docker compose down
    ```

### Opção 2: Via Gradle (Local)

1.  Certifique-se de ter o Java 21 configurado.
2.  Execute o comando:

    **Windows:**
    ```powershell
    ./gradlew bootRun
    ```

    **Linux/Mac:**
    ```bash
    ./gradlew bootRun
    ```

## Documentação da API

Com a aplicação rodando, acesse a interface do Swagger UI para testar os endpoints visualmente:

**[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Principais Endpoints

| Verbo  | Endpoint       | Descrição                                         |
| :----- | :------------- | :------------------------------------------------ |
| POST   | `/coupon`      | Cria um novo cupão (Status nasce como ACTIVE).    |
| GET    | `/coupon/{id}` | Retorna os detalhes de um cupão específico.       |
| DELETE | `/coupon/{id}` | Realiza a exclusão lógica (muda status para DELETED). |


## Executando os Testes

O projeto conta com uma suíte de testes que cobre desde a unidade até a integração (Jornada do Usuário).

Para rodar todos os testes:

**Windows:**
```powershell
./gradlew test
# Relatório: Análise da Adequação da Arquitetura ao Desafio Proposto

**Objetivo:** Avaliar se a arquitetura hexagonal / clean adotada no projeto Coupon API faz sentido considerando o escopo e os requisitos do desafio técnico.

---

## 1. Contexto do desafio

### 1.1 O que foi pedido (resumo)

- Foco em **Create** e **Delete** de cupons, não em um CRUD completo.
- Regras de negócio funcionando corretamente (ex.: não deletar duas vezes).
- Testar **comportamento**, não só tecnologia; a avaliação pode tentar “quebrar” as regras.
- Entregar projeto **funcional, organizado, com regras bem definidas e testáveis**.

### 1.2 Diretrizes explícitas fornecidas

As dicas do desafio orientavam explicitamente:

1. **“Matar” services genéricos** — classes focadas em uma única intenção (não um “CustomerService” com 20 métodos).
2. **Inversão de dependência** — a camada de application não deve importar JPA/Spring; usar interfaces (portas).
3. **Use case como maestro** — orquestrar o fluxo; regras de negócio na entidade (domain), não if/else no use case.
4. **Application agnóstica** — não depender de Web, terminal ou fila.
5. **Check-in antes de commitar:** um método público (execute), dependência só de interfaces, sem `import` de `org.springframework.web` ou `javax.persistence` na application.

Ou seja, o próprio enunciado **sugeria** uma separação em camadas, portas e domínio rico, ainda que não obrigasse uma arquitetura hexagonal completa.

### 1.3 Escopo técnico real

- **3 endpoints:** POST (criar), GET (buscar por ID), DELETE (soft delete).
- **1 agregado:** Cupom, com regras de criação (código, desconto, data) e de deleção (não deletar duas vezes).
- **1 persistência:** JPA/H2, sem requisitos de múltiplos bounded contexts ou integrações externas complexas.

---

## 2. O que foi implementado (resumo)

| Camada        | Elementos |
|---------------|-----------|
| **Domain**    | `Coupon` (modelo rico), `CouponStatus`, `BusinessException` — sem JPA/Spring. |
| **Application** | Portas (`SaveCouponPort`, `LoadCouponPort`), 3 use cases (Create, Delete, Get), `ResourceNotFoundException`. |
| **Infrastructure** | `CouponEntity`, `CouponRepository`, `CouponPersistenceAdapter`, controller, mapper, `GlobalExceptionHandler`, `UseCaseConfig`, DTOs e deserializador de data. |

Características: inversão de dependência (application só vê portas), um use case por intenção, regras no domínio, testes em várias camadas (domain, use case, controller, integração).

---

## 3. Análise: quando a arquitetura faz sentido para o desafio

### 3.1 Alinhamento com as diretrizes do desafio

- **“Matar services genéricos”** → Três use cases (Create, Delete, Get), cada um com um `execute`, atendem à ideia de “uma intenção por classe”.
- **Inversão de dependência** → Portas na application e adapter na infrastructure atendem à regra de “não importar JPA na application”.
- **Maestro vs. músico** → Use cases só orquestram; `Coupon.create()` e `Coupon.delete()` concentram as regras.
- **Application agnóstica** → Domain e application sem dependências de Spring Web ou JPA; o check-in sugerido é respeitado.

Conclusão: a arquitetura **traduz em código** exatamente os princípios pedidos nas dicas. Para um desafio que será avaliado por quem valoriza esses princípios, isso é um **diferencial positivo**.

### 3.2 Regras bem definidas e testáveis

- As regras (código 6 caracteres, desconto mínimo, data futura, não deletar duas vezes) estão no **domínio**, em pontos claros (`Coupon.create`, `Coupon.delete`).
- É possível testar essas regras com **testes unitários de domínio** sem subir Spring nem banco.
- Testes de **integração** (ex.: “deletar duas vezes” retorna 400) garantem o comportamento de ponta a ponta.
- O requisito de “testar comportamento, não só tecnologia” e “tentar quebrar as regras” é atendido: as regras estão explícitas e cobertas por testes em mais de um nível.

Aqui a separação **domain vs. infrastructure** ajuda diretamente: o núcleo é testável sem mocks de repositório.

### 3.3 Organização e manutenção

- Responsabilidades bem delimitadas: quem cria regra é o domain; quem persiste é o adapter; quem expõe HTTP é o controller.
- Inclusão de nova regra (ex.: “código não pode ser palavra bloqueada”) tem lugar óbvio: `Coupon.create(...)`.
- Novo caso de uso (ex.: listar ativos) segue o padrão: nova porta, novo use case, implementação no adapter.

Para um desafio que pede “projeto organizado”, a estrutura oferece **navegação e extensão previsíveis**.

---

## 4. Análise: quando a arquitetura pode ser questionada

### 4.1 Custo em relação ao escopo

- **Escopo pequeno:** 3 endpoints, 1 agregado, 1 repositório. Uma abordagem mais simples (ex.: Controller → Service → Repository, com entidade JPA já “rica”) também poderia implementar as mesmas regras e testes.
- **Número de artefatos:** várias classes (portas, use cases, adapter, entidade JPA, modelo de domínio, DTOs, handler de exceção, config). Para quem prefere “menos arquivos”, pode parecer excessivo.
- **Indireção:** uma requisição passa por Controller → UseCase → Port (interface) → Adapter → Repository. Em um sistema maior isso paga dividendos; em um desafio mínimo, pode ser visto como **overhead**.

Ou seja: para **só** cumprir “Create e Delete funcionando e testados”, uma arquitetura mais enxuta seria **suficiente**. A arquitetura atual faz **mais** do que o mínimo necessário.

### 4.2 Benefícios “futuros” pouco usados no desafio

- **Troca de persistência ou de canal (fila, CLI):** o desafio não exige outro canal nem outro banco. O benefício existe no desenho, mas não é exercitado no escopo.
- **Múltiplos bounded contexts / agregados:** há apenas um agregado (Cupom). Não há integrações complexas que justifiquem portas por necessidade imediata.

Assim, parte da justificativa clássica da hexagonal (múltiplos adaptadores, vários contextos) **não é exigida** pelo enunciado; é uma **escolha de design** que demonstra boas práticas.

### 4.3 Duplicação de conceitos

- Dois “modelos” de cupom: `Coupon` (domínio) e `CouponEntity` (JPA). O adapter faz a conversão. Para um único agregado simples, uma única entidade JPA com regras no próprio bean (como em muitos projetos Spring) reduziria código e conversões. A separação é **consciente** para manter o domain puro, mas não é **obrigatória** para o desafio.

---

## 5. Alternativa mais enxuta (comparação teórica)

Uma versão “mínima” que ainda atenderia ao enunciado poderia ser:

- **Controller** → **Service** (uma classe com `criar`, `deletar`, `buscarPorId`) → **Repository** (JPA).
- **Entidade JPA** com métodos de domínio (`create(...)`, `delete()`) e validações.
- Testes: unitários na entidade (regras) e de integração na API (fluxos e “quebrar regras”).
- Sem portas, sem modelo de domínio separado, sem use cases explícitos.

**Vantagens:** menos classes, menos indireção, entrega rápida.  
**Desvantagens:** não atende às dicas de “inversão de dependência” e “application agnóstica”; a entidade fica acoplada a JPA; o “service” tende a virar o “service genérico” que as dicas pediam para evitar.

Conclusão: essa alternativa **atende ao requisito funcional** (Create/Delete corretos e testáveis), mas **não atende às diretrizes de design** que o desafio sugeriu. A arquitetura adotada faz sentido **se** o avaliador considerar essas diretrizes como parte do critério de sucesso.

---

## 6. Conclusão e recomendação

### 6.1 Faz sentido usar toda essa arquitetura para o desafio?

- **Se o critério for só “funcional e testável”:** uma arquitetura mais simples seria suficiente; a hexagonal pode ser vista como **mais do que o estritamente necessário** para o tamanho do problema.
- **Se o critério incluir as dicas do desafio** (services focados, inversão de dependência, regras no domínio, application agnóstica): a arquitetura **faz sentido** e **demonstra** esses pontos de forma clara e verificável no código.

### 6.2 Veredicto

| Pergunta | Resposta |
|----------|----------|
| A arquitetura é **necessária** para o escopo do desafio? | **Não** — o escopo poderia ser atendido com menos camadas. |
| A arquitetura é **adequada** e **justificável** dado o enunciado e as dicas? | **Sim** — ela materializa os princípios pedidos e facilita regras claras e testes em várias camadas. |
| Há **overkill**? | Há um **custo extra** em número de classes e indireção; esse custo é **intencional** para alinhar com boas práticas e com o que o desafio sugeriu. |

Recomendação: **manter a arquitetura** no contexto deste desafio, desde que o objetivo seja mostrar domínio dos princípios (hexagonal, clean, testabilidade de comportamento). Em um projeto real com o mesmo escopo e sem expectativa de crescimento, uma versão mais enxuta seria defensável; em um **desafio técnico** que explicita “não fazer service genérico”, “inversão de dependência” e “regras no domínio”, a escolha atual é **consistente e bem fundamentada**.

---

## 7. Resumo em uma frase

A arquitetura é **mais elaborada do que o mínimo necessário** para três endpoints e um agregado, mas é **coerente e adequada** ao desafio na medida em que o enunciado e as dicas pediam exatamente esse tipo de organização (portas, domínio rico, application agnóstica) e regras testáveis de forma explícita.

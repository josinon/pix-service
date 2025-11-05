# Arquitetura de Validação - PIX Wallet

## 📋 Visão Geral

Este documento descreve a arquitetura de validação implementada no sistema PIX Wallet, seguindo os princípios de **Clean Architecture** com validações distribuídas em três camadas distintas.

## 🏗️ Camadas de Validação

### 1. **Camada de Apresentação** (Presentation Layer)
**Localização:** `org.pix.wallet.presentation.dto`

**Responsabilidade:** Validação sintática de entrada (Bean Validation)

**Tecnologia:** Jakarta Validation (`@NotNull`, `@NotBlank`, `@DecimalMin`, etc.)

**Exemplo:**
```java
public record PixTransferRequest(
    @NotNull UUID fromWalletId,
    @NotBlank String toPixKey,
    @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
```

**Quando usar:**
- Validar se campos obrigatórios estão presentes
- Verificar formatos básicos (email, números positivos)
- Validar tamanho mínimo/máximo de strings
- Garantir tipos de dados corretos

---

### 2. **Camada de Aplicação** (Application Layer)
**Localização:** `org.pix.wallet.application.service`

**Responsabilidade:** Validação de regras de negócio transversais e orquestração

**Componente Principal:** `WalletOperationValidator`

**Exemplo:**
```java
// Valida idempotência e estado da carteira
walletOperationValidator.validateIdempotency(walletId, idempotencyKey);
walletOperationValidator.validateWalletHasSufficientFunds(wallet, amount);
```

**Quando usar:**
- Validar idempotência de operações
- Verificar estado e saldo de carteiras
- Coordenar validações entre múltiplos agregados
- Garantir integridade transacional

---

### 3. **Camada de Domínio** (Domain Layer)
**Localização:** `org.pix.wallet.domain.validator`

**Responsabilidade:** Validação de regras de negócio específicas do domínio

**Componentes:**

#### **PixKeyValidator**
Valida formatos de chaves PIX conforme especificação do Banco Central:

| Tipo | Formato | Exemplo |
|------|---------|---------|
| CPF | 11 dígitos | `12345678901` |
| EMAIL | email@dominio.com | `user@example.com` |
| PHONE | +[11-14 dígitos] | `+5511999999999` |
| RANDOM | 32 caracteres hex | `a1b2c3d4e5f6...` |

**Métodos:**
- `validate(PixKeyType, String)` - Valida formato da chave
- `normalizeAndGenerate(PixKeyType, String)` - Normaliza ou gera chave aleatória

#### **TransferValidator**
Valida regras de transferência PIX:

**Regras de Negócio:**
- ✅ Valor > R$ 0,00
- ✅ Valor ≤ R$ 100.000,00 (limite máximo)
- ✅ Carteira origem ≠ Carteira destino
- ✅ Timestamp do webhook não pode ser futuro
- ✅ Tipos de evento: `CONFIRMED`, `REJECTED`, `PENDING`

**Métodos:**
- `validateAmount(BigDecimal)` - Valida valor da transferência
- `validateDifferentWallets(UUID, UUID)` - Impede auto-transferência
- `validateWebhookEvent(...)` - Valida dados do webhook
- `validateAndNormalizeEventType(String)` - Valida e normaliza tipo de evento

---

## 📦 Estrutura de Pacotes

```
org.pix.wallet
├── presentation/
│   ├── api/                    # Controllers REST
│   └── dto/                    # DTOs com @NotNull, @NotBlank
├── application/
│   └── service/                # Services com WalletOperationValidator
└── domain/
    ├── model/                  # Entidades de domínio
    └── validator/              # 🆕 Validadores de regras de negócio
        ├── PixKeyValidator.java
        ├── TransferValidator.java
        └── ValidationConstants.java
```

## 🔄 Fluxo de Validação

```
[HTTP Request]
     ↓
[1. Bean Validation] ← Presentation Layer (DTOs)
     ↓ @Valid
[Controller]
     ↓
[2. Service Layer] ← Application Layer (WalletOperationValidator)
     ↓
[3. Domain Validators] ← Domain Layer (PixKeyValidator, TransferValidator)
     ↓
[Business Logic]
     ↓
[HTTP Response]
```

### Exemplo Completo: Criação de Chave PIX

```java
// 1. Bean Validation (Presentation)
@PostMapping("/pix-keys")
public ResponseEntity<?> createPixKey(@Valid @RequestBody CreatePixKeyRequest request) {
    
    // 2. Application Validation
    walletOperationValidator.validateIdempotency(request.walletId(), idempotencyKey);
    
    // 3. Domain Validation
    String normalized = pixKeyValidator.normalizeAndGenerate(request.type(), request.value());
    pixKeyValidator.validate(request.type(), normalized);
    
    // Business logic...
    return ResponseEntity.ok(response);
}
```

## 🎯 Princípios de Design

### 1. **Separação de Responsabilidades**
Cada camada valida aspectos específicos:
- **Presentation:** Sintaxe (campos presentes, formatos básicos)
- **Application:** Orquestração (idempotência, coordenação entre agregados)
- **Domain:** Regras de negócio (formatos PIX, limites de transferência)

### 2. **Fail Fast**
Validações ocorrem o mais cedo possível no fluxo:
```
Request → Bean Validation → Controller → Service → Domain
   ❌        ❌                 ❌          ❌         ❌
```

### 3. **Mensagens Descritivas**
Todas as validações retornam mensagens claras:
```java
throw new IllegalArgumentException(
    "Invalid CPF format. Expected 11 digits, got: " + cpf
);
```

### 4. **Reutilização**
Validadores podem ser usados em múltiplos contextos:
```java
// Em PixKeyService
pixKeyValidator.validate(type, value);

// Em PixWebhookService
transferValidator.validateWebhookEvent(...);
```

### 5. **Testabilidade**
Validadores são componentes independentes, fáceis de testar:
```java
@Test
void shouldRejectInvalidCPF() {
    assertThrows(IllegalArgumentException.class, () -> 
        validator.validate(PixKeyType.CPF, "123")
    );
}
```

## 📊 Estatísticas de Cobertura

| Componente | Cobertura | Testes |
|------------|-----------|--------|
| PixKeyValidator | 96% | 17 testes |
| TransferValidator | 96% | 23 testes |
| WalletOperationValidator | 91% | Integrado |

**Total:** 40 testes unitários de validação

## 🔍 Constantes de Validação

Todas as strings mágicas e valores constantes foram extraídos para `ValidationConstants.java`:

```java
// ❌ Antes (hardcoded)
if (!cpf.matches("\\d{11}")) {
    throw new IllegalArgumentException("Invalid CPF...");
}

// ✅ Depois (centralizado)
if (!cpf.matches(CPF_PATTERN)) {
    throw new IllegalArgumentException(CPF_INVALID_FORMAT);
}
```

**Benefícios:**
- ✅ Fácil manutenção (alterar em um único lugar)
- ✅ Reutilização de mensagens consistentes
- ✅ Facilita internacionalização (i18n) futura
- ✅ Evita duplicação de código

## 🚀 Como Adicionar Novas Validações

### 1. Validação Sintática (Presentation)
Adicione anotações no DTO:
```java
public record MyRequest(
    @NotBlank @Size(max = 100) String name,
    @Email String email
) {}
```

### 2. Validação de Domínio
Adicione método no validador apropriado:
```java
@Component
public class MyDomainValidator {
    public void validate(MyEntity entity) {
        // Business rules here
    }
}
```

### 3. Adicione Constantes
```java
public static final class MyValidation {
    public static final String PATTERN = "...";
    public static final String ERROR_MESSAGE = "...";
}
```

### 4. Escreva Testes
```java
@Test
void shouldValidateMyRule() {
    // Arrange
    MyEntity valid = new MyEntity(...);
    
    // Act & Assert
    assertDoesNotThrow(() -> validator.validate(valid));
}
```

## 📚 Referências

- [Jakarta Bean Validation Specification](https://jakarta.ee/specifications/bean-validation/3.0/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [PIX Specifications - Brazilian Central Bank](https://www.bcb.gov.br/estabilidadefinanceira/pix)
- [Domain-Driven Design Validation](https://enterprisecraftsmanship.com/posts/validation-in-ddd/)

## 🤝 Contribuindo

Ao adicionar novas validações:
1. ✅ Escolha a camada correta (Presentation/Application/Domain)
2. ✅ Extraia constantes para `ValidationConstants`
3. ✅ Adicione JavaDoc com exemplos
4. ✅ Escreva testes unitários (mínimo 90% cobertura)
5. ✅ Use mensagens descritivas em português
6. ✅ Mantenha consistência com validações existentes

---

**Última atualização:** Novembro 2025  
**Versão:** 1.0  
**Autores:** PIX Wallet Team

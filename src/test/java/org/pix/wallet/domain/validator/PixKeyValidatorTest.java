package org.pix.wallet.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pix.wallet.domain.model.enums.PixKeyType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PixKeyValidator to ensure PIX key validation rules
 * are correctly enforced.
 */
@DisplayName("PixKeyValidator Unit Tests")
class PixKeyValidatorTest {

    private final PixKeyValidator validator = new PixKeyValidator();

    @Test
    @DisplayName("Should validate valid CPF")
    void shouldValidateValidCPF() {
        // Given
        String validCPF = "12345678901";

        // When/Then - should not throw
        validator.validate(PixKeyType.CPF, validCPF);
    }

    @Test
    @DisplayName("Should reject invalid CPF with wrong length")
    void shouldRejectInvalidCPFLength() {
        // Given
        String invalidCPF = "123456789"; // 9 digits instead of 11

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.CPF, invalidCPF))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid CPF format");
    }

    @Test
    @DisplayName("Should reject CPF with non-numeric characters")
    void shouldRejectCPFWithNonNumeric() {
        // Given
        String invalidCPF = "123.456.789-01";

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.CPF, invalidCPF))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid CPF format");
    }

    @Test
    @DisplayName("Should validate valid email")
    void shouldValidateValidEmail() {
        // Given
        String validEmail = "user@example.com";

        // When/Then - should not throw
        validator.validate(PixKeyType.EMAIL, validEmail);
    }

    @Test
    @DisplayName("Should reject invalid email without @")
    void shouldRejectEmailWithoutAt() {
        // Given
        String invalidEmail = "userexample.com";

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.EMAIL, invalidEmail))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("Should reject email that is too long")
    void shouldRejectTooLongEmail() {
        // Given
        String tooLongEmail = "a".repeat(100) + "@" + "b".repeat(30) + ".com"; // > 120 chars

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.EMAIL, tooLongEmail))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email too long");
    }

    @Test
    @DisplayName("Should validate valid phone")
    void shouldValidateValidPhone() {
        // Given
        String validPhone = "+5511987654321"; // 14 digits total

        // When/Then - should not throw
        validator.validate(PixKeyType.PHONE, validPhone);
    }

    @Test
    @DisplayName("Should reject phone without + prefix")
    void shouldRejectPhoneWithoutPlus() {
        // Given
        String invalidPhone = "5511987654321";

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.PHONE, invalidPhone))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid phone format");
    }

    @Test
    @DisplayName("Should reject phone with too few digits")
    void shouldRejectPhoneWithTooFewDigits() {
        // Given
        String invalidPhone = "+55119876"; // only 8 digits after +

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.PHONE, invalidPhone))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid phone format");
    }

    @Test
    @DisplayName("Should validate valid random PIX key")
    void shouldValidateValidRandomKey() {
        // Given
        String validRandom = "a1b2c3d4e5f67890abcdef1234567890"; // 32 hex chars

        // When/Then - should not throw
        validator.validate(PixKeyType.RANDOM, validRandom);
    }

    @Test
    @DisplayName("Should reject random key with wrong length")
    void shouldRejectRandomKeyWrongLength() {
        // Given
        String invalidRandom = "a1b2c3d4e5f6"; // only 12 chars

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.RANDOM, invalidRandom))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid random PIX key format");
    }

    @Test
    @DisplayName("Should reject random key with hyphens")
    void shouldRejectRandomKeyWithHyphens() {
        // Given
        String invalidRandom = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.RANDOM, invalidRandom))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid random PIX key format");
    }

    @Test
    @DisplayName("Should generate random PIX key")
    void shouldGenerateRandomPixKey() {
        // When
        String generated = validator.normalizeAndGenerate(PixKeyType.RANDOM, null);

        // Then
        assertThat(generated).isNotNull();
        assertThat(generated).hasSize(32);
        assertThat(generated).matches("[a-f0-9]{32}");
    }

    @Test
    @DisplayName("Should normalize and trim email")
    void shouldNormalizeEmail() {
        // Given
        String emailWithSpaces = "  user@example.com  ";

        // When
        String normalized = validator.normalizeAndGenerate(PixKeyType.EMAIL, emailWithSpaces);

        // Then
        assertThat(normalized).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should normalize and trim CPF")
    void shouldNormalizeCPF() {
        // Given
        String cpfWithSpaces = "  12345678901  ";

        // When
        String normalized = validator.normalizeAndGenerate(PixKeyType.CPF, cpfWithSpaces);

        // Then
        assertThat(normalized).isEqualTo("12345678901");
    }

    @Test
    @DisplayName("Should throw when PIX key type is null")
    void shouldThrowWhenTypeIsNull() {
        // When/Then
        assertThatThrownBy(() -> validator.validate(null, "somevalue"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PIX key type is required");
    }

    @Test
    @DisplayName("Should throw when PIX key value is null")
    void shouldThrowWhenValueIsNull() {
        // When/Then
        assertThatThrownBy(() -> validator.validate(PixKeyType.EMAIL, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PIX key value is required");
    }
}

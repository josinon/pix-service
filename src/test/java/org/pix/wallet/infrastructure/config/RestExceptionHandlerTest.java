package org.pix.wallet.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.pix.wallet.domain.exception.InsufficientFundsException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RestExceptionHandler to validate error response formatting
 * and HTTP status code mapping for different exception types.
 */
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void shouldHandleIllegalArgumentExceptionWith400Status() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid wallet ID");

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleIllegalArgument(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Invalid wallet ID");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleIllegalStateExceptionWith409Status() {
        // Given
        IllegalStateException exception = new IllegalStateException("Wallet already exists");

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleIllegalState(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
        assertThat(response.getBody().message()).isEqualTo("Wallet already exists");
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleInsufficientFundsExceptionWith409Status() {
        // Given
        InsufficientFundsException exception = new InsufficientFundsException(new java.math.BigDecimal("100.00"), new java.math.BigDecimal("150.00"));

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleInsufficientFunds(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(response.getBody().message()).contains("Insufficient balance");
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    void shouldHandleMethodArgumentNotValidExceptionWith400Status() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("request", "amount", "Amount must be > 0");
        FieldError fieldError2 = new FieldError("request", "pixKey", "PIX key is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleValidationErrors(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).contains("Invalid request parameters");
        assertThat(response.getBody().message()).contains("amount");
        assertThat(response.getBody().message()).contains("pixKey");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleMissingRequestHeaderExceptionWith400Status() {
        // Given
        MissingRequestHeaderException exception = new MissingRequestHeaderException(
            "Idempotency-Key",
            null
        );

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleMissingHeader(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Missing required header: Idempotency-Key");
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleGenericExceptionWith500Status() {
        // Given
        Exception exception = new RuntimeException("Unexpected database error");

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleGenericException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldIncludeTimestampInAllResponses() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Test error");

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleIllegalArgument(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(java.time.Instant.now());
    }

    @Test
    void shouldFormatValidationErrorsAsMap() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error = new FieldError("walletRequest", "ownerId", "Owner ID cannot be null");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<RestExceptionHandler.ErrorResponse> response = handler.handleValidationErrors(exception);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("ownerId");
        assertThat(response.getBody().message()).contains("Owner ID cannot be null");
    }
}

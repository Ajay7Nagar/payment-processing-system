package com.acme.payments.bootapi.idempotency;

import com.acme.payments.adapters.out.db.entity.IdempotencyRecordEntity;
import com.acme.payments.adapters.out.db.repo.IdempotencyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultIdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    @InjectMocks
    private DefaultIdempotencyService service;

    @Test
    void scopeKey_concatenates_parts_with_colon() {
        String result = service.scopeKey("m-123", "/path", "abc");
        assertThat(result).isEqualTo("m-123:/path:abc");
    }

    @Test
    void hashPayload_handles_null_payload() {
        String result = service.hashPayload(null);
        assertThat(result).hasSize(64);
    }

    @Test
    void hashPayload_returns_sha256_hex() {
        String result = service.hashPayload("hello");
        assertThat(result).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void hashPayload_wraps_exceptions_in_runtime() {
        try (var mocked = org.mockito.Mockito.mockStatic(java.security.MessageDigest.class)) {
            mocked.when(() -> java.security.MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new java.security.NoSuchAlgorithmException("boom"));

            assertThatThrownBy(() -> service.hashPayload("data"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("boom");
        }
    }

    @Test
    void findResponse_delegates_to_repository() {
        when(repository.findSnapshot("scope", "hash")).thenReturn("cached");

        String result = service.findResponse("scope", "hash");

        assertThat(result).isEqualTo("cached");
    }

    @Test
    void storeResponse_persists_entity_with_fields() {
        ArgumentCaptor<IdempotencyRecordEntity> captor = ArgumentCaptor.forClass(IdempotencyRecordEntity.class);

        service.storeResponse("scope", "hash", "{\"ok\":true}");

        verify(repository).save(captor.capture());
        IdempotencyRecordEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(getField(saved, "scopeKey")).isEqualTo("scope");
        assertThat(getField(saved, "requestHash")).isEqualTo("hash");
        assertThat(getField(saved, "responseSnapshot")).isEqualTo("{\"ok\":true}");
    }

    private Object getField(IdempotencyRecordEntity entity, String name) {
        try {
            var field = IdempotencyRecordEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(entity);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}



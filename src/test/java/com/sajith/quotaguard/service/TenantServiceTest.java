package com.sajith.quotaguard.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sajith.quotaguard.entity.Tenant;
import com.sajith.quotaguard.repository.TenantRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TenantServiceTest {

    @Test
    void repeatedLookups_hitPostgresOnce() {
        TenantRepository repo = mock(TenantRepository.class);
        when(repo.findByApiKeyHash(anyString())).thenReturn(Optional.of(new Tenant()));
        TenantService service = new TenantService(repo);

        for (int i = 0; i < 100; i++) {
            service.byApiKey("same-key");
        }

        verify(repo, times(1)).findByApiKeyHash(anyString());
    }
}

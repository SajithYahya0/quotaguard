package com.sajith.quotaguard.service;

import com.sajith.quotaguard.entity.Tenant;
import com.sajith.quotaguard.repository.TenantRepository;
import com.sajith.quotaguard.security.ApiKeys;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantService {

    /** The raw key leaves the server exactly once: in the create response. */
    public record CreatedTenant(Tenant tenant, String apiKey) {}

    private final TenantRepository repo;

    public TenantService(TenantRepository repo) {
        this.repo = repo;
    }

    public CreatedTenant create(String name, int capacity, double refillPerSec) {
        String apiKey = UUID.randomUUID().toString();
        Tenant t = new Tenant();
        t.setName(name);
        t.setCapacity(capacity);
        t.setRefillPerSec(refillPerSec);
        t.setApiKeyHash(ApiKeys.hash(apiKey));
        return new CreatedTenant(repo.save(t), apiKey);
    }

    public Optional<Tenant> byApiKey(String apiKey) {
        return repo.findByApiKeyHash(ApiKeys.hash(apiKey));
    }

    public Tenant updateLimits(Long id, int capacity, double refillPerSec) {
        Tenant t = repo
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        t.setCapacity(capacity);
        t.setRefillPerSec(refillPerSec);
        return repo.save(t);
    }
}

package com.sajith.quotagaurd.service;

import com.sajith.quotagaurd.entity.Tenant;
import com.sajith.quotagaurd.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
@Service
public class TenantService {
    private final TenantRepository repo;

    public TenantService(TenantRepository repo) {
        this.repo = repo;
    }

    public Tenant create(String name,int capacity,double refillPerSec){
        Tenant t = new Tenant();
        t.setName(name);
        t.setCapacity(capacity);
        t.setRefillPerSec(refillPerSec);
        t.setApiKey(UUID.randomUUID().toString());
        return repo.save(t);
    }

    public Optional<Tenant> byApiKey(String apiKey){
        return repo.findByApiKey(apiKey);
    }

    public Tenant updateLimits(Long id,int capacity,double refillPerSec){
        Tenant t = repo.findById(id).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Tenant not found")
        );
        t.setCapacity(capacity);t.setRefillPerSec(refillPerSec);
        return repo.save(t);

    }


























}

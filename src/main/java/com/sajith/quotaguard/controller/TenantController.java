package com.sajith.quotaguard.controller;

import com.sajith.quotaguard.entity.Tenant;
import com.sajith.quotaguard.limiter.RateLimiter;
import com.sajith.quotaguard.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final RateLimiter rateLimiter;

    public TenantController(TenantService tenantService, RateLimiter rateLimiter) {
        this.tenantService = tenantService;
        this.rateLimiter = rateLimiter;
    }

    public record CreateTenantRequest(String name, int capacity, double refillPerSec) {}

    public record UpdateLimitsRequest(int capacity, double refillPerSec) {}

    public record TenantResponse(Long id, String name, String apiKey, int capacity, double refillPerSec) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@RequestBody CreateTenantRequest req) {
        return toResponse(tenantService.create(req.name(), req.capacity(), req.refillPerSec()));
    }

    @PutMapping("/{id}/limits")
    public TenantResponse updateLimits(@PathVariable Long id, @RequestBody UpdateLimitsRequest req) {
        Tenant t = tenantService.updateLimits(id, req.capacity(), req.refillPerSec());
        rateLimiter.invalidate(t.getApiKey());
        return toResponse(t);
    }

    private TenantResponse toResponse(Tenant t) {
        return new TenantResponse(
            t.getId(),
            t.getName(),
            t.getApiKey(),
            t.getCapacity(),
            t.getRefillPerSec()
        );
    }
}

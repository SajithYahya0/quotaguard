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

    /** Only returned by create: the one time the raw key is shown. */
    public record CreatedTenantResponse(
        Long id,
        String name,
        String apiKey,
        int capacity,
        double refillPerSec
    ) {}

    public record TenantResponse(Long id, String name, int capacity, double refillPerSec) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedTenantResponse create(@RequestBody CreateTenantRequest req) {
        TenantService.CreatedTenant created = tenantService.create(
            req.name(),
            req.capacity(),
            req.refillPerSec()
        );
        Tenant t = created.tenant();
        return new CreatedTenantResponse(
            t.getId(),
            t.getName(),
            created.apiKey(),
            t.getCapacity(),
            t.getRefillPerSec()
        );
    }

    @PutMapping("/{id}/limits")
    public TenantResponse updateLimits(@PathVariable Long id, @RequestBody UpdateLimitsRequest req) {
        Tenant t = tenantService.updateLimits(id, req.capacity(), req.refillPerSec());
        rateLimiter.invalidate(t);
        return new TenantResponse(t.getId(), t.getName(), t.getCapacity(), t.getRefillPerSec());
    }
}

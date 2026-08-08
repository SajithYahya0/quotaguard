package com.sajith.quotagaurd.limiter;

import com.sajith.quotagaurd.entity.Tenant;
import com.sajith.quotagaurd.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final TenantService tenantService;
    private final InMemoryRateLimiter rateLimiterService;

    public RateLimitInterceptor(TenantService tenantService,
                                InMemoryRateLimiter rateLimiterService)
    {
        this.tenantService=tenantService;
        this.rateLimiterService=rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,Object handler)throws IOException{

        String apiKey = request.getHeader("X-API-Key");
        if(apiKey == null || apiKey.isBlank()){
            return reject(response,401,"Missing X-API-Key header");
        }
        Optional<Tenant> tenant = tenantService.byApiKey(apiKey);
        if(tenant.isEmpty())
        {
            return reject(response,401,"Invalid API Key");
        }

        Tenant t = tenant.get();
        if(!rateLimiterService.tryConsume(t))
        {
            response.setHeader("X-RateLimit-Limit",String.valueOf(t.getCapacity()));
            response.setHeader("Retry-After","1");
            return reject(response,429,"Rate limit exceeded");
        }


        return true;
    }
    private boolean reject(HttpServletResponse response,int status,String message)throws IOException{
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\""+message+"\"}");
        return false;
    }





}

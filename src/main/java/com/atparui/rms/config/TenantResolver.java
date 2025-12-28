package com.atparui.rms.config;

import com.atparui.rms.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TenantResolver {

    @Autowired
    private TenantConfigProperties tenantConfigProperties;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private GatewayAdminConfig gatewayAdminConfig;

    @Value("${multitenancy.default-tenant:gateway}")
    private String defaultTenant;

    @Value("${tenant.client-id:gateway-web}")
    private String defaultClientId;

    @Value("${tenant.client-secret:M5nP8qR2sT6uV9wX1yZ3aC4dE7fG0h}")
    private String defaultClientSecret;

    public Mono<String> resolveTenant(ServerWebExchange exchange) {
        String host = exchange.getRequest().getHeaders().getFirst("Host");

        // Check for gateway admin access
        if (host != null && (host.contains("admin.") || host.contains("gateway."))) {
            return Mono.just("gateway");
        }

        if (host != null) {
            host = host.toLowerCase();

            // Extract subdomain from host
            String subdomain = extractSubdomain(host);
            if (subdomain != null) {
                // Look up tenant by subdomain in database
                return tenantService.findBySubdomain(subdomain).map(tenant -> tenant.getTenantId()).switchIfEmpty(Mono.just(defaultTenant));
            }

            // Check if it's a direct domain mapping
            if (host.contains(".") && !host.contains("localhost")) {
                String domain = host.substring(0, host.lastIndexOf("."));
                return Mono.just(domain + "-realm");
            }
        }

        // Header fallback for mobile apps
        String tenantHeader = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        if (tenantHeader != null) {
            return Mono.just(tenantHeader);
        }

        return Mono.just(defaultTenant); // default
    }

    private String extractSubdomain(String host) {
        if (host.contains(".yourdomain.com")) {
            return host.substring(0, host.indexOf(".yourdomain.com"));
        }
        if (host.contains(".atparui.com")) {
            return host.substring(0, host.indexOf(".atparui.com"));
        }
        return null;
    }

    public Mono<String> getClientId(String tenantId, String clientType) {
        // Handle gateway admin realm
        if ("gateway".equals(tenantId)) {
            return Mono.just(gatewayAdminConfig.getClientId());
        }

        if ("mobile".equals(clientType)) {
            return Mono.just(tenantId.replace("-realm", "-mobile-app"));
        }

        // First check TenantConfigProperties (for default tenant from config)
        TenantConfigProperties.TenantConfig config = tenantConfigProperties.getTenantConfig(tenantId);
        if (config != null && config.getClientId() != null) {
            return Mono.just(config.getClientId());
        }

        // If not found in config, check database for dynamic tenant
        return tenantService
            .findTenant(tenantId)
            .map(tenant -> tenant.getClientId() != null ? tenant.getClientId() : defaultClientId)
            .switchIfEmpty(Mono.just(defaultClientId));
    }

    public Mono<String> getClientSecret(String tenantId) {
        // Handle gateway admin realm
        if ("gateway".equals(tenantId)) {
            return Mono.just(gatewayAdminConfig.getClientSecret());
        }

        // First check TenantConfigProperties (for default tenant from config)
        TenantConfigProperties.TenantConfig config = tenantConfigProperties.getTenantConfig(tenantId);
        if (config != null && config.getClientSecret() != null) {
            return Mono.just(config.getClientSecret());
        }

        // If not found in config, check database for dynamic tenant
        return tenantService
            .findTenant(tenantId)
            .map(tenant -> tenant.getClientSecret() != null ? tenant.getClientSecret() : defaultClientSecret)
            .switchIfEmpty(Mono.just(defaultClientSecret));
    }
}

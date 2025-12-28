package com.atparui.rms.web.filter;

import com.atparui.rms.config.TenantContext;
import com.atparui.rms.config.TenantResolver;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TenantFilter implements WebFilter, Ordered {

    private final TenantResolver tenantResolver;

    public TenantFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return tenantResolver
            .resolveTenant(exchange)
            .flatMap(tenantKey -> chain.filter(exchange).contextWrite(context -> context.put("tenantKey", tenantKey)))
            .doOnTerminate(TenantContext::clear);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

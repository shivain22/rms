package com.atparui.rms.config;

import com.atparui.rms.service.TenantService;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory;
import reactor.core.publisher.Mono;

@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.atparui.rms.repository",
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*search.*")
)
public class DatabaseBasedMultiTenantConfig extends AbstractR2dbcConfiguration {

    private final R2dbcProperties r2dbcProperties;
    private final TenantService tenantService;

    public DatabaseBasedMultiTenantConfig(R2dbcProperties r2dbcProperties, TenantService tenantService) {
        this.r2dbcProperties = r2dbcProperties;
        this.tenantService = tenantService;
    }

    @Bean
    @Primary
    @Override
    public ConnectionFactory connectionFactory() {
        return new TenantRoutingConnectionFactory(tenantService);
    }

    private static class TenantRoutingConnectionFactory extends AbstractRoutingConnectionFactory {

        private final TenantService tenantService;

        public TenantRoutingConnectionFactory(TenantService tenantService) {
            this.tenantService = tenantService;
        }

        @Override
        protected Mono<Object> determineCurrentLookupKey() {
            return Mono.justOrEmpty(TenantContext.getCurrentTenant());
        }

        @Override
        protected Mono<ConnectionFactory> determineTargetConnectionFactory() {
            return determineCurrentLookupKey()
                .cast(String.class)
                .flatMap(tenantService::getConnectionFactory)
                .switchIfEmpty(Mono.error(new IllegalStateException("No tenant context found")));
        }
    }
}

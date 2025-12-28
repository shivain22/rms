package com.atparui.rms.config;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import java.util.Map;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class TenantAwareConnectionFactory implements ConnectionFactory {

    private final ConnectionFactory defaultConnectionFactory;
    private final Map<String, ConnectionFactory> tenantConnectionFactories;

    public TenantAwareConnectionFactory(
        ConnectionFactory defaultConnectionFactory,
        Map<String, ConnectionFactory> tenantConnectionFactories
    ) {
        this.defaultConnectionFactory = defaultConnectionFactory;
        this.tenantConnectionFactories = tenantConnectionFactories;
    }

    @Override
    public Publisher<? extends Connection> create() {
        return Mono.deferContextual(contextView -> {
            // Always use default connection factory for now
            // Tenant-specific connections will be handled by TenantService when needed
            ConnectionFactory connectionFactory = tenantConnectionFactories.getOrDefault("default", defaultConnectionFactory);

            return Mono.from(connectionFactory.create());
        });
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return defaultConnectionFactory.getMetadata();
    }
}

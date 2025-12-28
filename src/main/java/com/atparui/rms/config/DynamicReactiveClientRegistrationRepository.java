package com.atparui.rms.config;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom ReactiveClientRegistrationRepository that dynamically resolves
 * client registrations based on the current tenant from ServerWebExchange.
 *
 * This ensures that the correct tenant-specific client ID is used for OAuth2 authentication
 * instead of the static default "web_app" from configuration.
 */
public class DynamicReactiveClientRegistrationRepository implements ReactiveClientRegistrationRepository {

    private final DynamicOAuth2ConfigService dynamicOAuth2ConfigService;

    public DynamicReactiveClientRegistrationRepository(DynamicOAuth2ConfigService dynamicOAuth2ConfigService) {
        this.dynamicOAuth2ConfigService = dynamicOAuth2ConfigService;
    }

    @Override
    public Mono<ClientRegistration> findByRegistrationId(String registrationId) {
        // Get ServerWebExchange from Reactor context
        return Mono.deferContextual(contextView -> {
            ServerWebExchange exchange = contextView.getOrDefault(ServerWebExchange.class, null);
            if (exchange != null) {
                return dynamicOAuth2ConfigService.getClientRegistration(exchange);
            }
            // Fallback: return empty if no exchange in context
            // This should not happen in normal flow, but provides safety
            return Mono.empty();
        });
    }
}

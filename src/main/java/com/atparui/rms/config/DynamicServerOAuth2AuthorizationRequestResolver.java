package com.atparui.rms.config;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom ServerOAuth2AuthorizationRequestResolver that dynamically resolves
 * client registrations based on the current tenant from ServerWebExchange.
 *
 * This ensures that the correct tenant-specific client ID is used for OAuth2 authentication
 * instead of the static default "web_app" from configuration.
 */
public class DynamicServerOAuth2AuthorizationRequestResolver implements ServerOAuth2AuthorizationRequestResolver {

    private final DynamicOAuth2ConfigService dynamicOAuth2ConfigService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DynamicServerOAuth2AuthorizationRequestResolver(DynamicOAuth2ConfigService dynamicOAuth2ConfigService) {
        this.dynamicOAuth2ConfigService = dynamicOAuth2ConfigService;
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
        return dynamicOAuth2ConfigService
            .getClientRegistration(exchange)
            .flatMap(clientRegistration -> buildAuthorizationRequest(exchange, clientRegistration));
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
        return resolve(exchange);
    }

    private Mono<OAuth2AuthorizationRequest> buildAuthorizationRequest(ServerWebExchange exchange, ClientRegistration clientRegistration) {
        // Get base URL from the request
        String baseUrl =
            exchange.getRequest().getURI().getScheme() +
            "://" +
            exchange.getRequest().getURI().getHost() +
            (exchange.getRequest().getURI().getPort() != -1 ? ":" + exchange.getRequest().getURI().getPort() : "");

        // Generate state parameter (CSRF token)
        String state = generateState();

        // Build the authorization request
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
            .clientId(clientRegistration.getClientId())
            .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
            .redirectUri(baseUrl + "/login/oauth2/code/" + clientRegistration.getRegistrationId())
            .scopes(clientRegistration.getScopes())
            .state(state)
            .attributes(attributes -> {
                attributes.put("registration_id", clientRegistration.getRegistrationId());
            });

        return Mono.just(builder.build());
    }

    private String generateState() {
        // Generate a secure random state token
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

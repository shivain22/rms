package com.atparui.rms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
public class DynamicOAuth2ConfigService {

    @Autowired
    private TenantResolver tenantResolver;

    @Value("${multitenancy.keycloak.base-url:https://auth.atparui.com}")
    private String baseKeycloakUrl;

    public Mono<ClientRegistration> getClientRegistration(ServerWebExchange exchange) {
        String clientType = detectClientType(exchange);

        return tenantResolver
            .resolveTenant(exchange)
            .flatMap(tenantId ->
                Mono.zip(tenantResolver.getClientId(tenantId, clientType), getClientSecret(tenantId, clientType)).map(tuple -> {
                    String clientId = tuple.getT1();
                    String clientSecret = tuple.getT2();
                    String realmUrl = baseKeycloakUrl + "/realms/" + tenantId;
                    return ClientRegistration.withRegistrationId("oidc")
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .scope("openid", "profile", "email", "offline_access")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .authorizationUri(realmUrl + "/protocol/openid-connect/auth")
                        .tokenUri(realmUrl + "/protocol/openid-connect/token")
                        .userInfoUri(realmUrl + "/protocol/openid-connect/userinfo")
                        .jwkSetUri(realmUrl + "/protocol/openid-connect/certs")
                        .issuerUri(realmUrl)
                        .build();
                })
            );
    }

    private String detectClientType(ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        if (userAgent != null && (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone"))) {
            return "mobile";
        }
        return "web";
    }

    private Mono<String> getClientSecret(String tenantId, String clientType) {
        if ("mobile".equals(clientType)) {
            return Mono.just(""); // Public client - empty string instead of null
        }
        return tenantResolver.getClientSecret(tenantId);
    }
}

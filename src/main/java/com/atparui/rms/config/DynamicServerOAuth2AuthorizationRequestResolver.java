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
        String path = exchange.getRequest().getURI().getPath();

        // OAuth2 callback endpoints (/login/oauth2/code/**) should NOT trigger authorization request building
        // They are handled by Spring Security's OAuth2 callback handler
        // Only /oauth2/authorization/** endpoints should build authorization requests
        boolean isOAuth2Callback = path.startsWith("/login/oauth2/code/");

        if (isOAuth2Callback) {
            // This is the OAuth2 callback - don't build authorization request, let Spring Security handle it
            return Mono.empty();
        }

        // For /oauth2/authorization/** endpoints, proceed with building authorization request
        boolean isOAuth2Authorization = path.startsWith("/oauth2/authorization/");

        if (isOAuth2Authorization) {
            // Let OAuth2 authorization flow proceed normally
            return dynamicOAuth2ConfigService
                .getClientRegistration(exchange)
                .flatMap(clientRegistration -> buildAuthorizationRequest(exchange, clientRegistration));
        }

        // Don't redirect API requests - let the authentication entry point handle them with 401
        // This prevents CORS issues when browser follows redirects to Keycloak
        boolean isApiRequest =
            path.startsWith("/api/") || path.startsWith("/management/") || path.startsWith("/services/") || path.startsWith("/v3/api-docs");

        // Check if request accepts JSON (typical for API calls)
        String acceptHeader = exchange.getRequest().getHeaders().getFirst("Accept");
        boolean acceptsJson = acceptHeader != null && (acceptHeader.contains("application/json") || acceptHeader.contains("*/*"));

        // Check if request is from XMLHttpRequest or fetch API
        String xRequestedWith = exchange.getRequest().getHeaders().getFirst("X-Requested-With");
        boolean isAjaxRequest = "XMLHttpRequest".equals(xRequestedWith);

        // Return empty for API requests to prevent OAuth2 redirect
        // The authentication entry point will handle these with 401
        if (isApiRequest || (acceptsJson && isAjaxRequest)) {
            return Mono.empty();
        }

        // For browser requests (non-API, non-OAuth2), proceed with OAuth2 flow
        return dynamicOAuth2ConfigService
            .getClientRegistration(exchange)
            .flatMap(clientRegistration -> buildAuthorizationRequest(exchange, clientRegistration));
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
        return resolve(exchange);
    }

    private Mono<OAuth2AuthorizationRequest> buildAuthorizationRequest(ServerWebExchange exchange, ClientRegistration clientRegistration) {
        // Get base URL from the request, prefer X-Forwarded headers for proxy scenarios
        String scheme = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = exchange.getRequest().getURI().getScheme();
        }

        String host = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
        if (host == null) {
            host = exchange.getRequest().getURI().getHost();
        }

        // Determine the correct port for OAuth2 redirect URI
        // Priority: Detect reverse proxy > Use standard ports > Handle local dev ports
        int requestPort = exchange.getRequest().getURI().getPort();
        String forwardedPort = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Port");
        String forwardedHost = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");

        // Check if we're behind a reverse proxy
        boolean isReverseProxy = forwardedPort != null || forwardedHost != null || forwardedProto != null;

        int redirectPort = -1; // -1 means don't append port (use standard port for scheme)

        if (isReverseProxy) {
            // Behind reverse proxy: use forwarded port or standard port for scheme
            // In production, we should never use port 9000/9060
            if (forwardedPort != null) {
                try {
                    int port = Integer.parseInt(forwardedPort);
                    // Only append port if it's not a standard port
                    // Always ignore port 9000/9060 in production (likely misconfigured proxy)
                    if (port != 443 && port != 80 && port != 9000 && port != 9060) {
                        redirectPort = port;
                    }
                    // If port is 9000/9060 or standard port, use standard port (don't append)
                } catch (NumberFormatException e) {
                    // Invalid port, use standard port for scheme
                }
            }
            // Also check if request port is 9000/9060 (shouldn't happen in production, but handle it)
            if (requestPort == 9000 || requestPort == 9060) {
                // Ignore dev ports in production - use standard port
                redirectPort = -1;
            }
            // If no forwarded port or standard port, redirectPort remains -1 (no port in URL)
        } else {
            // Local development: handle dev ports
            if (requestPort == 9000 || requestPort == 9060) {
                // Request from webpack dev server: OAuth callback should go to gateway port
                // Gateway serves both UI and API, so use gateway port (9293 on host)
                redirectPort = 9293;
            } else if (requestPort == 8080 || requestPort == 9293 || requestPort == -1) {
                // Gateway container port (8080) or host port (9293) or no port: use gateway port
                redirectPort = requestPort == -1 ? 9293 : requestPort;
            } else {
                // Other port: use it
                redirectPort = requestPort;
            }
        }

        // Build base URL for redirect
        String baseUrl = scheme + "://" + host;
        // Only append port if it's not a standard port (80 for HTTP, 443 for HTTPS)
        if (redirectPort != -1 && redirectPort != 443 && redirectPort != 80) {
            baseUrl += ":" + redirectPort;
        }

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

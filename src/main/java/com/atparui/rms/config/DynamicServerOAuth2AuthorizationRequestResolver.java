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
        // IMPORTANT: The redirect URI must point to the BACKEND server, not the frontend!
        // The backend handles the OAuth2 callback at /login/oauth2/code/oidc
        // After authentication, the backend will redirect to the frontend

        // Get the backend server's address from the request
        String scheme = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = exchange.getRequest().getURI().getScheme();
        }
        if (scheme == null) {
            scheme = "http"; // Default to http for localhost
        }

        String host = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
        String requestHost = exchange.getRequest().getURI().getHost();
        boolean isRequestLocalhost = "localhost".equals(requestHost) || "127.0.0.1".equals(requestHost);

        // Only use forwarded host if we're not in localhost context (avoid production domain in local dev)
        if (host == null || (isRequestLocalhost && !host.contains("localhost") && !host.contains("127.0.0.1"))) {
            host = requestHost;
        }

        // Determine the backend port for the redirect URI
        int requestPort = exchange.getRequest().getURI().getPort();
        String forwardedPort = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Port");
        String forwardedHost = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");

        // Check if we're in local development (localhost or 127.0.0.1)
        boolean isLocalhost = "localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host);

        // Determine backend port for redirect URI
        int backendPort = -1;

        // Check if we're behind a reverse proxy
        boolean isReverseProxy =
            forwardedPort != null || (forwardedHost != null && !isLocalhost) || (forwardedProto != null && !isLocalhost);

        if (isReverseProxy && !isLocalhost) {
            // Behind reverse proxy in production: use forwarded port or standard port for scheme
            if (forwardedPort != null) {
                try {
                    int port = Integer.parseInt(forwardedPort);
                    // Only append port if it's not a standard port
                    if (port != 443 && port != 80) {
                        backendPort = port;
                    }
                } catch (NumberFormatException e) {
                    // Invalid port, use standard port for scheme
                    backendPort = -1;
                }
            }
        } else if (isLocalhost) {
            // Local development: use the actual backend request port
            // Backend is typically on 8080 (container) or 9293 (host) or other port
            if (requestPort == 8080 || requestPort == 9293 || requestPort == 8082 || requestPort > 0) {
                backendPort = requestPort;
            } else if (requestPort == -1) {
                // No port in URI, default to common backend ports
                backendPort = 8080; // Default Spring Boot port
            }
        } else {
            // Production (not localhost): use request port or standard port
            if (requestPort == -1) {
                backendPort = -1;
            } else if (requestPort != 443 && requestPort != 80) {
                backendPort = requestPort;
            } else {
                backendPort = -1;
            }
        }

        // Build base URL for OAuth2 redirect URI (must point to BACKEND)
        String baseUrl = scheme + "://" + host;
        // Only append port if it's not a standard port (80 for HTTP, 443 for HTTPS)
        if (backendPort != -1 && backendPort != 443 && backendPort != 80) {
            baseUrl += ":" + backendPort;
        }

        // Generate state parameter (CSRF token)
        String state = generateState();

        // Store the frontend origin (from Origin/Referer headers) for redirect after authentication
        // This is separate from the backend redirect URI above
        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        String referer = exchange.getRequest().getHeaders().getFirst("Referer");
        String originalOriginValue = null;

        if (origin != null && (origin.contains("localhost:9000") || origin.contains("127.0.0.1:9000"))) {
            originalOriginValue = origin;
        } else if (referer != null && (referer.contains("localhost:9000") || referer.contains("127.0.0.1:9000"))) {
            try {
                java.net.URI refererUri = java.net.URI.create(referer);
                String refererBase = refererUri.getScheme() + "://" + refererUri.getHost();
                if (refererUri.getPort() != -1 && refererUri.getPort() != 80 && refererUri.getPort() != 443) {
                    refererBase += ":" + refererUri.getPort();
                }
                originalOriginValue = refererBase;
            } catch (Exception e) {
                // Ignore
            }
        }

        // Make final for use in lambda
        final String finalOriginalOrigin = originalOriginValue;

        // Build the authorization request
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
            .clientId(clientRegistration.getClientId())
            .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
            .redirectUri(baseUrl + "/login/oauth2/code/" + clientRegistration.getRegistrationId())
            .scopes(clientRegistration.getScopes())
            .state(state)
            .attributes(attributes -> {
                attributes.put("registration_id", clientRegistration.getRegistrationId());
                if (finalOriginalOrigin != null) {
                    attributes.put("original_origin", finalOriginalOrigin);
                }
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

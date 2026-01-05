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
        // Check Origin header first (for CORS requests from webpack dev server)
        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        String referer = exchange.getRequest().getHeaders().getFirst("Referer");

        // If Origin header indicates localhost:9000 (webpack dev server), use that for redirect
        String scheme = null;
        String host = null;
        int redirectPort = -1;

        if (origin != null && (origin.contains("localhost:9000") || origin.contains("127.0.0.1:9000"))) {
            // Request from webpack dev server - redirect back to localhost:9000
            try {
                java.net.URI originUri = java.net.URI.create(origin);
                scheme = originUri.getScheme();
                host = originUri.getHost();
                redirectPort = originUri.getPort();
            } catch (Exception e) {
                // Fall through to default logic
            }
        } else if (referer != null && (referer.contains("localhost:9000") || referer.contains("127.0.0.1:9000"))) {
            // Fallback to Referer header
            try {
                java.net.URI refererUri = java.net.URI.create(referer);
                scheme = refererUri.getScheme();
                host = refererUri.getHost();
                redirectPort = refererUri.getPort();
            } catch (Exception e) {
                // Fall through to default logic
            }
        }

        // If not set from Origin/Referer, use request headers/URI
        // But prioritize localhost detection to avoid using production domain
        if (scheme == null) {
            String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
            // Only use forwarded proto if we're not in localhost context
            String requestHost = exchange.getRequest().getURI().getHost();
            boolean isRequestLocalhost = "localhost".equals(requestHost) || "127.0.0.1".equals(requestHost);

            if (forwardedProto != null && !isRequestLocalhost) {
                scheme = forwardedProto;
            } else {
                scheme = exchange.getRequest().getURI().getScheme();
            }
        }

        if (host == null) {
            String forwardedHost = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
            String requestHost = exchange.getRequest().getURI().getHost();
            boolean isRequestLocalhost = "localhost".equals(requestHost) || "127.0.0.1".equals(requestHost);

            // Only use forwarded host if we're not in localhost context (avoid production domain in local dev)
            if (
                forwardedHost != null && !isRequestLocalhost && !forwardedHost.contains("localhost") && !forwardedHost.contains("127.0.0.1")
            ) {
                host = forwardedHost;
            } else {
                host = requestHost;
            }
        }

        // Determine the correct port for OAuth2 redirect URI
        // Priority: Origin/Referer header > Detect reverse proxy > Use standard ports > Handle local dev ports
        int requestPort = exchange.getRequest().getURI().getPort();
        String forwardedPort = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Port");
        String forwardedHost = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");

        // Check if we're in local development (localhost or 127.0.0.1)
        boolean isLocalhost = "localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host);

        // If redirectPort not set from Origin/Referer, determine it
        if (redirectPort == -1) {
            // Check if we're behind a reverse proxy
            boolean isReverseProxy =
                forwardedPort != null || (forwardedHost != null && !isLocalhost) || (forwardedProto != null && !isLocalhost);

            if (isReverseProxy && !isLocalhost) {
                // Behind reverse proxy in production: use forwarded port or standard port for scheme
                // In production, we should never use port 9000/9060
                if (forwardedPort != null) {
                    try {
                        int port = Integer.parseInt(forwardedPort);
                        // Only append port if it's not a standard port
                        // Always ignore port 9000/9060 in production (likely misconfigured proxy)
                        if (port != 443 && port != 80 && port != 9000 && port != 9060) {
                            redirectPort = port;
                        } else {
                            redirectPort = -1; // Use standard port
                        }
                    } catch (NumberFormatException e) {
                        // Invalid port, use standard port for scheme
                        redirectPort = -1;
                    }
                } else {
                    redirectPort = -1;
                }
                // Also check if request port is 9000/9060 (shouldn't happen in production, but handle it)
                if (requestPort == 9000 || requestPort == 9060) {
                    // Ignore dev ports in production - use standard port
                    redirectPort = -1;
                }
            } else if (isLocalhost) {
                // Local development: use the actual request port
                if (requestPort == 9000 || requestPort == 9060) {
                    // Request from webpack dev server: redirect back to the same port
                    redirectPort = requestPort;
                } else if (requestPort == 8080 || requestPort == 9293 || requestPort == -1) {
                    // Gateway container port (8080) or host port (9293) or no port: use gateway port
                    redirectPort = requestPort == -1 ? 9293 : requestPort;
                } else {
                    // Other port: use it
                    redirectPort = requestPort;
                }
            } else {
                // Production (not localhost, not behind proxy): use request port or standard port
                if (requestPort == 9000 || requestPort == 9060) {
                    // Ignore dev ports in production
                    redirectPort = -1;
                } else if (requestPort == -1) {
                    // No port specified: use standard port for scheme
                    redirectPort = -1;
                } else {
                    // Use the request port
                    redirectPort = requestPort;
                }
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

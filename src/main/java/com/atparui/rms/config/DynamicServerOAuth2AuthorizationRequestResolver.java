package com.atparui.rms.config;

import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(DynamicServerOAuth2AuthorizationRequestResolver.class);

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
        //
        // NOTE: Backend is always in production mode, but we need to detect if frontend
        // is running locally (npm start on localhost:9000) vs production (built and served)

        // Check if request is coming from localhost:9000 or localhost:9060 (frontend dev server running via npm start)
        String originHeader = exchange.getRequest().getHeaders().getFirst("Origin");
        String refererHeader = exchange.getRequest().getHeaders().getFirst("Referer");
        boolean isFrontendLocal =
            (originHeader != null &&
                (originHeader.contains("localhost:9000") ||
                    originHeader.contains("localhost:9060") ||
                    originHeader.contains("127.0.0.1:9000") ||
                    originHeader.contains("127.0.0.1:9060"))) ||
            (refererHeader != null &&
                (refererHeader.contains("localhost:9000") ||
                    refererHeader.contains("localhost:9060") ||
                    refererHeader.contains("127.0.0.1:9000") ||
                    refererHeader.contains("127.0.0.1:9060")));

        // Get the backend server's address from the request
        String scheme = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = exchange.getRequest().getURI().getScheme();
        }
        if (scheme == null) {
            scheme = "https"; // Default to https for production
        }

        String host;
        String requestHost = exchange.getRequest().getURI().getHost();
        String forwardedHost = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");

        // IMPORTANT: The redirect URI to Keycloak must ALWAYS point to the production backend
        // (rmsgateway.atparui.com), not localhost, because the backend is deployed in production.
        // After authentication, the backend will redirect to the frontend (localhost:9000 if local).

        // Always use production backend domain for redirect URI (backend is always in production)
        // Prefer X-Forwarded-Host (set by reverse proxy) over request host
        if (forwardedHost != null && !forwardedHost.contains("localhost") && !forwardedHost.contains("127.0.0.1")) {
            host = forwardedHost;
        } else {
            host = requestHost;
        }

        if (isFrontendLocal) {
            LOG.info(
                "Frontend running locally (localhost:9000) detected - redirect URI will point to production backend: {}://{}",
                scheme,
                host
            );
            LOG.info("After authentication, backend will redirect to localhost:9000");
        } else {
            LOG.info("Frontend in production mode - using production domain for backend redirect URI: {}://{}", scheme, host);
        }

        // Determine the backend port for the redirect URI
        int requestPort = exchange.getRequest().getURI().getPort();
        String forwardedPort = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Port");
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");

        // Backend is always in production, so determine port based on production setup
        boolean isRequestLocalhost = "localhost".equals(requestHost) || "127.0.0.1".equals(requestHost);

        // Determine backend port for redirect URI (always production backend)
        int backendPort = -1;

        // Check if we're behind a reverse proxy (always true in production)
        String forwardedHostCheck = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
        boolean isReverseProxy = forwardedPort != null || (forwardedHostCheck != null) || (forwardedProto != null);

        if (isReverseProxy) {
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
        } else {
            // Not behind proxy: use request port or standard port
            if (requestPort == -1) {
                backendPort = -1;
            } else if (requestPort != 443 && requestPort != 80) {
                backendPort = requestPort;
            } else {
                backendPort = -1;
            }
        }

        // Build base URL for OAuth2 redirect URI (must point to BACKEND)
        String baseUrlValue = scheme + "://" + host;
        // Only append port if it's not a standard port (80 for HTTP, 443 for HTTPS)
        if (backendPort != -1 && backendPort != 443 && backendPort != 80) {
            baseUrlValue += ":" + backendPort;
        }
        final String baseUrl = baseUrlValue;

        // Generate state parameter (CSRF token)
        String state = generateState();

        // Store the frontend origin (from Origin/Referer headers) for redirect after authentication
        // This is separate from the backend redirect URI above
        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        String referer = exchange.getRequest().getHeaders().getFirst("Referer");
        String originalOriginValue = null;

        // Priority 1: Check Origin header for localhost:9000 or localhost:9060
        if (
            origin != null &&
            (origin.contains("localhost:9000") ||
                origin.contains("localhost:9060") ||
                origin.contains("127.0.0.1:9000") ||
                origin.contains("127.0.0.1:9060"))
        ) {
            originalOriginValue = origin;
            LOG.info("Detected frontend URL from Origin header: {}", originalOriginValue);
        }
        // Priority 2: Check Referer header for localhost:9000 or localhost:9060
        else if (
            referer != null &&
            (referer.contains("localhost:9000") ||
                referer.contains("localhost:9060") ||
                referer.contains("127.0.0.1:9000") ||
                referer.contains("127.0.0.1:9060"))
        ) {
            try {
                java.net.URI refererUri = java.net.URI.create(referer);
                String refererBase = refererUri.getScheme() + "://" + refererUri.getHost();
                if (refererUri.getPort() != -1 && refererUri.getPort() != 80 && refererUri.getPort() != 443) {
                    refererBase += ":" + refererUri.getPort();
                }
                originalOriginValue = refererBase;
                LOG.info("Detected frontend URL from Referer header: {}", originalOriginValue);
            } catch (Exception e) {
                LOG.warn("Failed to parse Referer header: {}", referer, e);
            }
        }
        // Priority 3: If frontend is local (detected earlier) but no Origin/Referer detected, try to determine port
        // This handles cases where the request doesn't have Origin/Referer headers (e.g., direct navigation)
        // Since backend is always in prod mode, we check if frontend is local based on isFrontendLocal flag
        else if (isFrontendLocal) {
            String frontendScheme = "http"; // Always use http for localhost frontend
            // Try to determine port from headers - default to 9000 if can't determine
            String frontendPort = "9000";
            if (origin != null && origin.contains(":9060")) {
                frontendPort = "9060";
            } else if (referer != null && referer.contains(":9060")) {
                frontendPort = "9060";
            }
            originalOriginValue = frontendScheme + "://localhost:" + frontendPort;
            LOG.info(
                "Frontend detected as local but no Origin/Referer headers - defaulting to localhost:{}: {}",
                frontendPort,
                originalOriginValue
            );
        }

        // Store the original frontend URL in the session for retrieval after authentication
        // This is needed because the OAuth2 callback from Keycloak won't have Origin/Referer headers
        final String finalOriginalOrigin = originalOriginValue;

        return exchange
            .getSession()
            .flatMap(session -> {
                // Store original frontend URL in session
                if (finalOriginalOrigin != null) {
                    session.getAttributes().put("OAUTH2_ORIGINAL_FRONTEND_URL", finalOriginalOrigin);
                    LOG.info("Stored frontend URL in session: {}", finalOriginalOrigin);
                } else {
                    LOG.warn(
                        "No frontend URL detected - Origin: {}, Referer: {}, Backend localhost: {}",
                        origin,
                        referer,
                        isRequestLocalhost
                    );
                }
                // Also store if backend is on localhost (for fallback) - use the already declared isRequestLocalhost
                session.getAttributes().put("OAUTH2_BACKEND_IS_LOCALHOST", isRequestLocalhost);
                LOG.info("Stored backend localhost flag in session: {}", isRequestLocalhost);

                // Build the authorization request
                String redirectUri = baseUrl + "/login/oauth2/code/" + clientRegistration.getRegistrationId();
                LOG.info("Building OAuth2 authorization request with redirect URI: {}", redirectUri);
                LOG.info(
                    "Request details - Origin: {}, Referer: {}, Request Host: {}, Forwarded Host: {}, IsFrontendLocal: {}",
                    originHeader,
                    refererHeader,
                    requestHost,
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host"),
                    isFrontendLocal
                );

                OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .clientId(clientRegistration.getClientId())
                    .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                    .redirectUri(redirectUri)
                    .scopes(clientRegistration.getScopes())
                    .state(state)
                    .attributes(attributes -> {
                        attributes.put("registration_id", clientRegistration.getRegistrationId());
                        if (finalOriginalOrigin != null) {
                            attributes.put("original_origin", finalOriginalOrigin);
                        }
                    });

                return Mono.just(builder.build());
            });
    }

    private String generateState() {
        // Generate a secure random state token
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

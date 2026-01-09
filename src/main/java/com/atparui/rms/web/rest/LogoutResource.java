package com.atparui.rms.web.rest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing global OIDC logout.
 * Supports both session-based OAuth2 authentication and JWT Bearer token authentication.
 */
@RestController
public class LogoutResource {

    private static final Logger LOG = LoggerFactory.getLogger(LogoutResource.class);

    private final ReactiveClientRegistrationRepository registrationRepository;

    public LogoutResource(ReactiveClientRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    /**
     * {@code POST  /api/logout} : logout the current user.
     *
     * @param authentication the current authentication (can be OAuth2AuthenticationToken or JwtAuthenticationToken).
     * @param oidcUser the OIDC user (may be null for JWT authentication).
     * @param request a {@link ServerHttpRequest} request.
     * @param session the current {@link WebSession}.
     * @return status {@code 200 (OK)} and a body with a global logout URL.
     */
    @PostMapping("/api/logout")
    public Mono<Map<String, String>> logout(
        @CurrentSecurityContext(expression = "authentication") Authentication authentication,
        @AuthenticationPrincipal(errorOnInvalidType = false) Object principal,
        ServerHttpRequest request,
        WebSession session
    ) {
        LOG.debug("Logout requested. Authentication type: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");

        // Handle JWT Bearer token authentication
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            LOG.debug("Processing JWT-based logout");
            Jwt jwt = jwtAuth.getToken();
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;

            return session
                .invalidate()
                .then(buildLogoutUrlFromJwt(request, jwt, issuer))
                .onErrorResume(throwable -> {
                    LOG.warn("Error during JWT logout: {}", throwable.getMessage());
                    return Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request)));
                });
        }

        // Handle OAuth2 session-based authentication
        if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
            LOG.debug("Processing OAuth2 session-based logout");

            if (!(principal instanceof OidcUser oidcUser)) {
                LOG.debug("Principal is not OidcUser, returning fallback URL");
                return session.invalidate().then(Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request))));
            }

            String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
            if (registrationId == null) {
                return session.invalidate().then(Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request))));
            }

            OidcIdToken idToken = oidcUser.getIdToken();
            if (idToken == null) {
                return session.invalidate().then(Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request))));
            }

            return session
                .invalidate()
                .then(
                    registrationRepository
                        .findByRegistrationId(registrationId)
                        .switchIfEmpty(Mono.error(new IllegalStateException("Client registration not found: " + registrationId)))
                        .map(clientRegistration -> prepareLogoutUri(request, clientRegistration, idToken))
                )
                .onErrorResume(throwable -> {
                    LOG.warn("Error during OAuth2 logout: {}", throwable.getMessage());
                    return Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request)));
                });
        }

        // Handle case where authentication is null or unknown type
        LOG.debug("Unknown authentication type or null, returning fallback URL");
        return session.invalidate().then(Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request))));
    }

    private Mono<Map<String, String>> buildLogoutUrlFromJwt(ServerHttpRequest request, Jwt jwt, String issuer) {
        if (issuer == null) {
            return Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request)));
        }

        // Try to find a matching client registration by issuer
        return registrationRepository
            .findByRegistrationId("oidc")
            .switchIfEmpty(Mono.empty())
            .map(clientRegistration -> {
                Object endSessionEndpoint = clientRegistration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint");
                if (endSessionEndpoint == null) {
                    // Construct end_session_endpoint from issuer
                    String endSessionUrl = issuer + (issuer.endsWith("/") ? "" : "/") + "protocol/openid-connect/logout";
                    return buildLogoutUrl(request, endSessionUrl, jwt.getTokenValue());
                }
                return buildLogoutUrl(request, endSessionEndpoint.toString(), jwt.getTokenValue());
            })
            .defaultIfEmpty(Map.of("logoutUrl", getFallbackRedirectUrl(request)));
    }

    private Map<String, String> buildLogoutUrl(ServerHttpRequest request, String endSessionEndpoint, String idTokenHint) {
        StringBuilder logoutUrl = new StringBuilder();
        logoutUrl.append(endSessionEndpoint);
        String originUrl = getOriginUrl(request);
        String encodedOriginUrl = URLEncoder.encode(originUrl, StandardCharsets.UTF_8);
        LOG.debug("Building logout URL with post_logout_redirect_uri: {} (encoded: {})", originUrl, encodedOriginUrl);
        logoutUrl.append("?id_token_hint=").append(idTokenHint).append("&post_logout_redirect_uri=").append(encodedOriginUrl);
        return Map.of("logoutUrl", logoutUrl.toString());
    }

    private Map<String, String> prepareLogoutUri(ServerHttpRequest request, ClientRegistration clientRegistration, OidcIdToken idToken) {
        StringBuilder logoutUrl = new StringBuilder();

        Object endSessionEndpoint = clientRegistration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint");
        if (endSessionEndpoint == null) {
            // If no end_session_endpoint, just redirect to home
            return Map.of("logoutUrl", getFallbackRedirectUrl(request));
        }

        logoutUrl.append(endSessionEndpoint.toString());

        String originUrl = getOriginUrl(request);
        String encodedOriginUrl = URLEncoder.encode(originUrl, StandardCharsets.UTF_8);
        LOG.debug("Building OAuth2 logout URL with post_logout_redirect_uri: {} (encoded: {})", originUrl, encodedOriginUrl);

        logoutUrl.append("?id_token_hint=").append(idToken.getTokenValue()).append("&post_logout_redirect_uri=").append(encodedOriginUrl);

        return Map.of("logoutUrl", logoutUrl.toString());
    }

    private String getOriginUrl(ServerHttpRequest request) {
        String origin = request.getHeaders().getOrigin();
        if (origin != null && !origin.isEmpty()) {
            return origin;
        }
        // Fallback: construct from request URI
        return getFallbackRedirectUrl(request);
    }

    private String getFallbackRedirectUrl(ServerHttpRequest request) {
        // Construct redirect URL from request
        String scheme = request.getURI().getScheme();
        String host = request.getURI().getHost();
        int port = request.getURI().getPort();

        StringBuilder url = new StringBuilder();
        url.append(scheme != null ? scheme : "http").append("://");
        url.append(host != null ? host : "localhost");
        if (port > 0 && port != 80 && port != 443) {
            url.append(":").append(port);
        }
        url.append("/");

        return url.toString();
    }
}

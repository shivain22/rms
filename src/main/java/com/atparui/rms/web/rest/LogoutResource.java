package com.atparui.rms.web.rest;

import java.util.Map;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing global OIDC logout.
 */
@RestController
public class LogoutResource {

    private final ReactiveClientRegistrationRepository registrationRepository;

    public LogoutResource(ReactiveClientRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    /**
     * {@code POST  /api/logout} : logout the current user.
     *
     * @param oAuth2AuthenticationToken the OAuth2 authentication token.
     * @param oidcUser the OIDC user.
     * @param request a {@link ServerHttpRequest} request.
     * @param session the current {@link WebSession}.
     * @return status {@code 200 (OK)} and a body with a global logout URL.
     */
    @PostMapping("/api/logout")
    public Mono<Map<String, String>> logout(
        @CurrentSecurityContext(expression = "authentication") OAuth2AuthenticationToken oAuth2AuthenticationToken,
        @AuthenticationPrincipal OidcUser oidcUser,
        ServerHttpRequest request,
        WebSession session
    ) {
        // Handle case where authentication token or user is null (already logged out)
        if (oAuth2AuthenticationToken == null || oidcUser == null) {
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
            .onErrorResume(throwable -> Mono.just(Map.of("logoutUrl", getFallbackRedirectUrl(request))));
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

        logoutUrl.append("?id_token_hint=").append(idToken.getTokenValue()).append("&post_logout_redirect_uri=").append(originUrl);

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

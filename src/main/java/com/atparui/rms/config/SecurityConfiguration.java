package com.atparui.rms.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import com.atparui.rms.security.AuthoritiesConstants;
import com.atparui.rms.security.SecurityUtils;
import com.atparui.rms.security.oauth2.AudienceValidator;
import com.atparui.rms.web.filter.SpaWebFilter;
import com.atparui.rms.web.filter.TenantFilter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.web.filter.reactive.CookieCsrfFilter;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${jhipster.cors.allowed-origins:#{null}}")
    private String allowedOrigins;

    private final ReactiveClientRegistrationRepository clientRegistrationRepository;
    private final TenantFilter tenantFilter;
    private final DynamicOAuth2ConfigService dynamicOAuth2ConfigService;
    private final CorsConfigurationSource corsConfigurationSource;

    // See https://github.com/jhipster/generator-jhipster/issues/18868
    // We don't use a distributed cache or the user selected cache implementation here on purpose
    private final Cache<String, Mono<Jwt>> users = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofHours(1))
        .recordStats()
        .build();

    public SecurityConfiguration(
        ReactiveClientRegistrationRepository clientRegistrationRepository,
        JHipsterProperties jHipsterProperties,
        TenantFilter tenantFilter,
        DynamicOAuth2ConfigService dynamicOAuth2ConfigService,
        CorsConfigurationSource corsConfigurationSource
    ) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.jHipsterProperties = jHipsterProperties;
        this.tenantFilter = tenantFilter;
        this.dynamicOAuth2ConfigService = dynamicOAuth2ConfigService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(
                new NegatedServerWebExchangeMatcher(
                    new OrServerWebExchangeMatcher(pathMatchers("/app/**", "/i18n/**", "/content/**", "/swagger-ui/**"))
                )
            )
            .cors(withDefaults())
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                    // See https://stackoverflow.com/q/74447118/65681
                    .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
            )
            // See https://github.com/spring-projects/spring-security/issues/5766
            .addFilterAt(new CookieCsrfFilter(), SecurityWebFiltersOrder.REACTOR_CONTEXT)
            .addFilterBefore(tenantFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new SpaWebFilter(), SecurityWebFiltersOrder.HTTPS_REDIRECT)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(frameOptions -> frameOptions.mode(Mode.DENY))
                    .referrerPolicy(referrer ->
                        referrer.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    )
                    .permissionsPolicy(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .authorizeExchange(authz ->
                // prettier-ignore
                authz
                    .pathMatchers("/").permitAll()
                    .pathMatchers("/index.html").permitAll()
                    .pathMatchers("/*.*").permitAll()
                    .pathMatchers("/api/authenticate").permitAll()
                    .pathMatchers("/api/auth-info").permitAll()
                    .pathMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .pathMatchers("/api/**").authenticated()
                    // OAuth2 endpoints must be public for redirect flow
                    .pathMatchers("/oauth2/**").permitAll()
                    .pathMatchers("/login/**").permitAll()
                    // microfrontend resources are loaded by webpack without authentication, they need to be public
                    .pathMatchers("/services/*/*.js").permitAll()
                    .pathMatchers("/services/*/*.txt").permitAll()
                    .pathMatchers("/services/*/*.json").permitAll()
                    .pathMatchers("/services/*/*.js.map").permitAll()
                    .pathMatchers("/services/*/management/health/readiness").permitAll()
                    .pathMatchers("/services/*/v3/api-docs/**").permitAll()
                    .pathMatchers("/services/**").authenticated()
                    .pathMatchers("/v3/api-docs/**").permitAll()
                    .pathMatchers("/management/health").permitAll()
                    .pathMatchers("/management/health/**").permitAll()
                    .pathMatchers("/management/info").permitAll()
                    .pathMatchers("/management/prometheus").permitAll()
                    .pathMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .oauth2Login(oauth2 ->
                oauth2
                    .authorizationRequestResolver(authorizationRequestResolver(this.clientRegistrationRepository))
                    .authenticationSuccessHandler(oauth2AuthenticationSuccessHandler())
            )
            .oauth2Client(withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(apiAuthenticationEntryPoint()));
        return http.build();
    }

    private ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
        ReactiveClientRegistrationRepository clientRegistrationRepository
    ) {
        // Use dynamic resolver that resolves client registration based on tenant
        DynamicServerOAuth2AuthorizationRequestResolver dynamicResolver = new DynamicServerOAuth2AuthorizationRequestResolver(
            dynamicOAuth2ConfigService
        );

        // Fallback to default resolver if dynamic resolution fails
        DefaultServerOAuth2AuthorizationRequestResolver defaultResolver = new DefaultServerOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository
        );
        if (this.issuerUri.contains("auth0.com")) {
            defaultResolver.setAuthorizationRequestCustomizer(authorizationRequestCustomizer());
        }

        // Return dynamic resolver as primary
        return dynamicResolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer ->
            customizer.authorizationRequestUri(uriBuilder ->
                uriBuilder.queryParam("audience", jHipsterProperties.getSecurity().getOauth2().getAudience()).build()
            );
    }

    /**
     * Custom OAuth2 authentication success handler that redirects to the frontend (port 9000)
     * instead of the backend (port 8082) after successful login.
     */
    private RedirectServerAuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        return new RedirectServerAuthenticationSuccessHandler("/") {
            @Override
            public Mono<Void> onAuthenticationSuccess(
                WebFilterExchange webFilterExchange,
                org.springframework.security.core.Authentication authentication
            ) {
                ServerWebExchange exchange = webFilterExchange.getExchange();

                // Get the original request to determine the frontend URL
                String scheme = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
                if (scheme == null) {
                    scheme = exchange.getRequest().getURI().getScheme();
                }

                String host = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
                if (host == null) {
                    host = exchange.getRequest().getURI().getHost();
                }

                // Determine the correct port for redirect
                // Priority: X-Forwarded-Port > request port > default based on scheme
                int requestPort = exchange.getRequest().getURI().getPort();
                String forwardedPort = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Port");

                // Check if we're behind a reverse proxy
                boolean isReverseProxy =
                    forwardedPort != null ||
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host") != null ||
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto") != null;

                int frontendPort = -1; // -1 means don't append port (use standard port for scheme)

                if (isReverseProxy) {
                    // Behind reverse proxy: use forwarded port or standard port for scheme
                    if (forwardedPort != null) {
                        try {
                            int port = Integer.parseInt(forwardedPort);
                            // Only append port if it's not a standard port
                            if (port != 443 && port != 80) {
                                frontendPort = port;
                            }
                        } catch (NumberFormatException e) {
                            // Invalid port, use standard port for scheme
                        }
                    }
                    // If no forwarded port or standard port, frontendPort remains -1 (no port in URL)
                } else {
                    // Local development: check for dev ports (9000, 9060) or container port (8080)
                    if (requestPort == 9000 || requestPort == 9060) {
                        frontendPort = requestPort;
                    } else if (requestPort == 8080 || requestPort == -1) {
                        // Container port or default: use 9000 for local dev
                        frontendPort = 9000;
                    } else {
                        // Other port: use it
                        frontendPort = requestPort;
                    }
                }

                // Build frontend URL
                // Check if we have a configured frontend URL from CORS allowed origins
                String frontendUrl = null;
                if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                    // Use the first allowed origin as the frontend URL (typically the dashboard)
                    String[] origins = allowedOrigins.split(",");
                    if (origins.length > 0) {
                        frontendUrl = origins[0].trim();
                        // Remove trailing slash if present
                        if (frontendUrl.endsWith("/")) {
                            frontendUrl = frontendUrl.substring(0, frontendUrl.length() - 1);
                        }
                        frontendUrl += "/";
                    }
                }

                // Fallback: build from request if no configured frontend URL
                if (frontendUrl == null) {
                    frontendUrl = scheme + "://" + host;
                    // Only append port if it's not a standard port (80 for HTTP, 443 for HTTPS)
                    if (frontendPort != -1 && frontendPort != 443 && frontendPort != 80) {
                        frontendUrl += ":" + frontendPort;
                    }
                    frontendUrl += "/";
                }

                // Redirect to frontend
                // Note: Spring Security automatically saves the session when the response is committed
                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                exchange.getResponse().getHeaders().setLocation(URI.create(frontendUrl));
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * Custom authentication entry point that returns 401 for API requests
     * instead of redirecting to OAuth2 login, which causes CORS issues.
     * This also ensures CORS headers are properly added to the response.
     */
    private ServerAuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (exchange, ex) -> {
            String path = exchange.getRequest().getURI().getPath();

            // OAuth2 callback endpoints should NEVER trigger the entry point since they're permitAll()
            // But if they do, we must NOT redirect (that would cause a loop)
            // The callback must be processed by Spring Security's OAuth2 callback handler
            boolean isOAuth2Callback = path.startsWith("/login/oauth2/code/");

            if (isOAuth2Callback) {
                // Callback endpoint - must not redirect, let Spring Security handle it
                // Complete the response normally to allow the filter chain to continue
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }

            // For /oauth2/authorization/** endpoints, redirect to Keycloak is normal
            // But these should also be permitAll() and not trigger entry point
            // If they do, redirect normally
            boolean isOAuth2Authorization = path.startsWith("/oauth2/authorization/");

            if (isOAuth2Authorization) {
                // This shouldn't happen, but if it does, let it proceed normally
                // The resolver will handle building the authorization request
                String redirectUrl = "/oauth2/authorization/oidc";
                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                exchange.getResponse().getHeaders().setLocation(URI.create(redirectUrl));
                return exchange.getResponse().setComplete();
            }

            // Check if this is an API request
            boolean isApiRequest =
                path.startsWith("/api/") ||
                path.startsWith("/management/") ||
                path.startsWith("/services/") ||
                path.startsWith("/v3/api-docs");

            // Check if request accepts JSON (typical for API calls)
            String acceptHeader = exchange.getRequest().getHeaders().getFirst("Accept");
            boolean acceptsJson =
                acceptHeader != null && (acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE) || acceptHeader.contains("*/*"));

            // Check if request is from XMLHttpRequest or fetch API
            String xRequestedWith = exchange.getRequest().getHeaders().getFirst("X-Requested-With");
            boolean isAjaxRequest = "XMLHttpRequest".equals(xRequestedWith);

            // Return 401 for API requests to avoid CORS issues with Keycloak redirects
            if (isApiRequest || (acceptsJson && isAjaxRequest)) {
                // Apply CORS headers before setting the response
                return applyCorsHeaders(exchange)
                    .then(
                        Mono.fromRunnable(() -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        })
                    )
                    .then(exchange.getResponse().setComplete());
            }

            // Check if this is a static resource request (SPA routes)
            // These should be served by SpaWebFilter and should not redirect to login
            // Note: These paths are permitAll(), so the entry point shouldn't normally be called
            // But if it is (e.g., due to session issues), we should not redirect to login
            boolean isStaticResource =
                path.equals("/") ||
                path.equals("/index.html") ||
                path.startsWith("/app/") ||
                path.startsWith("/i18n/") ||
                path.startsWith("/content/") ||
                path.startsWith("/swagger-ui/") ||
                (!path.contains(".") &&
                    !path.startsWith("/api") &&
                    !path.startsWith("/management") &&
                    !path.startsWith("/services") &&
                    !path.startsWith("/oauth2") &&
                    !path.startsWith("/login"));

            if (isStaticResource) {
                // Static resource request - these are permitAll() so shouldn't require authentication
                // If entry point is called for these, it's likely a session/configuration issue
                // For static resources, we should NOT redirect to login
                // Instead, we need to allow the request to be handled by the SpaWebFilter
                // Since entry points must complete the response, we can't let the chain continue
                // The solution: return 200 OK without redirect, which allows the browser
                // to make a fresh request that will be handled correctly by SpaWebFilter
                // Note: This is a workaround - ideally these paths shouldn't trigger the entry point
                // The browser will get 200 OK and can then request the resource again
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }

            // For other non-API requests (browser requests), redirect to OAuth2 authorization endpoint
            // This endpoint will handle the redirect to Keycloak server-side
            String redirectUrl = "/oauth2/authorization/oidc";
            exchange.getResponse().setStatusCode(HttpStatus.FOUND);
            exchange.getResponse().getHeaders().setLocation(URI.create(redirectUrl));
            return exchange.getResponse().setComplete();
        };
    }

    /**
     * Apply CORS headers to the response using the configured CORS configuration source.
     */
    private Mono<Void> applyCorsHeaders(org.springframework.web.server.ServerWebExchange exchange) {
        if (corsConfigurationSource != null) {
            org.springframework.web.cors.CorsConfiguration corsConfig = corsConfigurationSource.getCorsConfiguration(exchange);
            if (corsConfig != null) {
                HttpHeaders headers = exchange.getResponse().getHeaders();
                String origin = exchange.getRequest().getHeaders().getFirst("Origin");

                // Set allowed origin
                if (origin != null) {
                    String allowedOrigin = corsConfig.checkOrigin(origin);
                    if (allowedOrigin != null) {
                        headers.setAccessControlAllowOrigin(allowedOrigin);
                    }
                }

                // Set credentials
                if (corsConfig.getAllowCredentials() != null && corsConfig.getAllowCredentials()) {
                    headers.setAccessControlAllowCredentials(true);
                }

                // Set allowed methods
                if (corsConfig.getAllowedMethods() != null && !corsConfig.getAllowedMethods().isEmpty()) {
                    headers.setAccessControlAllowMethods(
                        corsConfig.getAllowedMethods().stream().map(String::toUpperCase).map(HttpMethod::valueOf).toList()
                    );
                }

                // Set allowed headers
                if (corsConfig.getAllowedHeaders() != null && !corsConfig.getAllowedHeaders().isEmpty()) {
                    headers.setAccessControlAllowHeaders(corsConfig.getAllowedHeaders());
                }

                // Set exposed headers
                if (corsConfig.getExposedHeaders() != null && !corsConfig.getExposedHeaders().isEmpty()) {
                    headers.setAccessControlExposeHeaders(corsConfig.getExposedHeaders());
                }

                // Set max age
                if (corsConfig.getMaxAge() != null) {
                    headers.setAccessControlMaxAge(corsConfig.getMaxAge());
                }
            }
        }
        return Mono.empty();
    }

    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            new Converter<Jwt, Flux<GrantedAuthority>>() {
                @Override
                public Flux<GrantedAuthority> convert(Jwt jwt) {
                    return Flux.fromIterable(SecurityUtils.extractAuthorityFromClaims(jwt.getClaims()));
                }
            }
        );
        jwtAuthenticationConverter.setPrincipalClaimName(PREFERRED_USERNAME);
        return jwtAuthenticationConverter;
    }

    /**
     * Map authorities from "groups" or "roles" claim in ID Token.
     *
     * @return a {@link ReactiveOAuth2UserService} that has the groups from the IdP.
     */
    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

        return userRequest -> {
            // Delegate to the default implementation for loading a user
            return delegate
                .loadUser(userRequest)
                .map(user -> {
                    Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

                    user
                        .getAuthorities()
                        .forEach(authority -> {
                            if (authority instanceof OidcUserAuthority) {
                                OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;
                                mappedAuthorities.addAll(
                                    SecurityUtils.extractAuthorityFromClaims(oidcUserAuthority.getUserInfo().getClaims())
                                );
                            }
                        });

                    return new DefaultOidcUser(mappedAuthorities, user.getIdToken(), user.getUserInfo(), PREFERRED_USERNAME);
                });
        };
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(ReactiveClientRegistrationRepository registrations) {
        Mono<ClientRegistration> clientRegistration = registrations.findByRegistrationId("oidc");

        return clientRegistration
            .map(oidc ->
                createJwtDecoder(
                    oidc.getProviderDetails().getIssuerUri(),
                    oidc.getProviderDetails().getJwkSetUri(),
                    oidc.getProviderDetails().getUserInfoEndpoint().getUri()
                )
            )
            .block();
    }

    private ReactiveJwtDecoder createJwtDecoder(String issuerUri, String jwkSetUri, String userInfoUri) {
        NimbusReactiveJwtDecoder jwtDecoder = new NimbusReactiveJwtDecoder(jwkSetUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jHipsterProperties.getSecurity().getOauth2().getAudience());
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return new ReactiveJwtDecoder() {
            @Override
            public Mono<Jwt> decode(String token) throws JwtException {
                return jwtDecoder.decode(token).flatMap(jwt -> enrich(token, jwt));
            }

            private Mono<Jwt> enrich(String token, Jwt jwt) {
                // Only look up user information if identity claims are missing
                if (jwt.hasClaim("given_name") && jwt.hasClaim("family_name")) {
                    return Mono.just(jwt);
                }
                // Get user info from `users` cache if present
                return Optional.ofNullable(users.getIfPresent(jwt.getSubject())).orElseGet(() -> // Retrieve user info from OAuth provider if not already loaded
                    WebClient.create()
                        .get()
                        .uri(userInfoUri)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .map(userInfo ->
                            Jwt.withTokenValue(jwt.getTokenValue())
                                .subject(jwt.getSubject())
                                .audience(jwt.getAudience())
                                .headers(headers -> headers.putAll(jwt.getHeaders()))
                                .claims(claims -> {
                                    String username = userInfo.get("preferred_username").toString();
                                    // special handling for Auth0
                                    if (userInfo.get("sub").toString().contains("|") && username.contains("@")) {
                                        userInfo.put("email", username);
                                    }
                                    // Allow full name in a name claim - happens with Auth0
                                    if (userInfo.get("name") != null) {
                                        String[] name = userInfo.get("name").toString().split("\\s+");
                                        if (name.length > 0) {
                                            userInfo.put("given_name", name[0]);
                                            userInfo.put("family_name", String.join(" ", Arrays.copyOfRange(name, 1, name.length)));
                                        }
                                    }
                                    claims.putAll(userInfo);
                                })
                                .claims(claims -> claims.putAll(jwt.getClaims()))
                                .build()
                        )
                        // Put user info into the `users` cache
                        .doOnNext(newJwt -> users.put(jwt.getSubject(), Mono.just(newJwt)))
                );
            }
        };
    }
}

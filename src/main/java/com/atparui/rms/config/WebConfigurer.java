package com.atparui.rms.config;

import com.atparui.rms.web.filter.SpaWebFilter;
import com.atparui.rms.web.rest.errors.ExceptionTranslator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.reactive.ResourceHandlerRegistrationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.web.filter.reactive.CachingHttpHeadersFilter;
import tech.jhipster.web.rest.errors.ReactiveWebExceptionHandler;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
public class WebConfigurer implements WebFluxConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(WebConfigurer.class);

    private final JHipsterProperties jHipsterProperties;
    private final Environment environment;

    public WebConfigurer(JHipsterProperties jHipsterProperties, Environment environment) {
        this.jHipsterProperties = jHipsterProperties;
        this.environment = environment;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = jHipsterProperties.getCors();

        // Override with environment variable if present
        // Check for CORS_ALLOWED_ORIGINS or JHIPSTER_CORS_ALLOWED_ORIGINS
        String corsAllowedOrigins = environment.getProperty("CORS_ALLOWED_ORIGINS");
        if (corsAllowedOrigins == null) {
            corsAllowedOrigins = environment.getProperty("JHIPSTER_CORS_ALLOWED_ORIGINS");
        }
        if (corsAllowedOrigins == null) {
            corsAllowedOrigins = environment.getProperty("jhipster.cors.allowed-origins");
        }

        if (StringUtils.hasText(corsAllowedOrigins)) {
            LOG.info("Using CORS allowed origins from environment variable: {}", corsAllowedOrigins);
            config.setAllowedOrigins(java.util.Arrays.asList(corsAllowedOrigins.split(",")));
        }

        // Override allowed methods if present in environment
        String corsAllowedMethods = environment.getProperty("CORS_ALLOWED_METHODS");
        if (corsAllowedMethods == null) {
            corsAllowedMethods = environment.getProperty("JHIPSTER_CORS_ALLOWED_METHODS");
        }
        if (corsAllowedMethods == null) {
            corsAllowedMethods = environment.getProperty("jhipster.cors.allowed-methods");
        }
        if (StringUtils.hasText(corsAllowedMethods)) {
            LOG.info("Using CORS allowed methods from environment variable: {}", corsAllowedMethods);
            if ("*".equals(corsAllowedMethods)) {
                config.addAllowedMethod("*");
            } else {
                config.setAllowedMethods(java.util.Arrays.asList(corsAllowedMethods.split(",")));
            }
        }

        // Override allowed headers if present in environment
        String corsAllowedHeaders = environment.getProperty("CORS_ALLOWED_HEADERS");
        if (corsAllowedHeaders == null) {
            corsAllowedHeaders = environment.getProperty("JHIPSTER_CORS_ALLOWED_HEADERS");
        }
        if (corsAllowedHeaders == null) {
            corsAllowedHeaders = environment.getProperty("jhipster.cors.allowed-headers");
        }
        if (StringUtils.hasText(corsAllowedHeaders)) {
            LOG.info("Using CORS allowed headers from environment variable: {}", corsAllowedHeaders);
            if ("*".equals(corsAllowedHeaders)) {
                config.addAllowedHeader("*");
            } else {
                config.setAllowedHeaders(java.util.Arrays.asList(corsAllowedHeaders.split(",")));
            }
        }

        if (!CollectionUtils.isEmpty(config.getAllowedOrigins()) || !CollectionUtils.isEmpty(config.getAllowedOriginPatterns())) {
            LOG.debug("Registering CORS filter with allowed origins: {}", config.getAllowedOrigins());
            source.registerCorsConfiguration("/api/**", config);
            source.registerCorsConfiguration("/management/**", config);
            source.registerCorsConfiguration("/v3/api-docs", config);
            source.registerCorsConfiguration("/swagger-ui/**", config);
            source.registerCorsConfiguration("/*/api/**", config);
            source.registerCorsConfiguration("/services/*/api/**", config);
            source.registerCorsConfiguration("/*/management/**", config);
            // Add CORS for OAuth2 endpoints
            source.registerCorsConfiguration("/oauth2/**", config);
            source.registerCorsConfiguration("/login/**", config);
        } else {
            LOG.warn("CORS is not configured - no allowed origins or patterns found");
        }
        return source;
    }

    // TODO: remove when this is supported in spring-boot
    @Bean
    HandlerMethodArgumentResolver reactivePageableHandlerMethodArgumentResolver() {
        return new ReactivePageableHandlerMethodArgumentResolver();
    }

    // TODO: remove when this is supported in spring-boot
    @Bean
    HandlerMethodArgumentResolver reactiveSortHandlerMethodArgumentResolver() {
        return new ReactiveSortHandlerMethodArgumentResolver();
    }

    @Bean
    @Order(-2) // The handler must have precedence over WebFluxResponseStatusExceptionHandler and Spring Boot's ErrorWebExceptionHandler
    public WebExceptionHandler problemExceptionHandler(ObjectMapper mapper, ExceptionTranslator problemHandling) {
        return new ReactiveWebExceptionHandler(problemHandling, mapper);
    }

    @Bean
    ResourceHandlerRegistrationCustomizer registrationCustomizer() {
        // Disable built-in cache control to use our custom filter instead
        return registration -> registration.setCacheControl(null);
    }

    @Bean
    @Profile(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
    public CachingHttpHeadersFilter cachingHttpHeadersFilter() {
        // Use a cache filter that only match selected paths
        return new CachingHttpHeadersFilter(TimeUnit.DAYS.toMillis(jHipsterProperties.getHttp().getCache().getTimeToLiveInDays()));
    }

    /**
     * Register SpaWebFilter as a global WebFilter that runs before Spring Security.
     * This ensures that static resources (like / and /index.html) are handled
     * even when excluded from the security chain.
     */
    @Bean
    @Order(-100) // Run before Spring Security
    public WebFilter spaWebFilter() {
        return new SpaWebFilter();
    }

    /**
     * Request logging filter to track all incoming API requests.
     * This helps debug authentication and endpoint access issues.
     */
    @Bean
    @Order(-50) // Run early but after SpaWebFilter
    public WebFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            String method = exchange.getRequest().getMethod().name();

            // Log API requests
            if (path.startsWith("/api") || path.startsWith("/management")) {
                LOG.info("=== Incoming API Request ===");
                LOG.info("Method: {}, Path: {}", method, path);
                LOG.info("Query: {}", exchange.getRequest().getURI().getQuery());
                LOG.info("Headers: {}", exchange.getRequest().getHeaders().toSingleValueMap());

                // Check for Authorization header
                String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
                if (authHeader != null) {
                    LOG.info("Authorization header present: {}...", authHeader.length() > 50 ? authHeader.substring(0, 50) : authHeader);
                } else {
                    LOG.warn("No Authorization header in request");
                }
            }

            return chain
                .filter(exchange)
                .doOnSuccess(v -> {
                    if (path.startsWith("/api") || path.startsWith("/management")) {
                        LOG.info("API Request completed: {} {}", method, path);
                    }
                })
                .doOnError(error -> {
                    if (path.startsWith("/api") || path.startsWith("/management")) {
                        LOG.error("API Request failed: {} {} - {}", method, path, error.getMessage(), error);
                    }
                });
        };
    }
}

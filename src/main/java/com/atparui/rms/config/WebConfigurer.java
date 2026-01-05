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

        // Development-friendly: If backend is running on localhost, automatically allow localhost:9000
        // This allows frontend development even when backend is in prod profile
        try {
            java.net.InetAddress localhost = java.net.InetAddress.getLocalHost();
            String hostAddress = localhost.getHostAddress();
            String hostName = localhost.getHostName();
            boolean isLocalhost =
                "127.0.0.1".equals(hostAddress) ||
                "localhost".equals(hostName) ||
                hostName.contains("localhost") ||
                hostAddress.startsWith("127.");

            if (isLocalhost) {
                // Add localhost:9000 and localhost:9060 to allowed origins if not already present
                java.util.List<String> allowedOrigins = config.getAllowedOrigins();
                if (allowedOrigins == null) {
                    allowedOrigins = new java.util.ArrayList<>();
                }
                boolean hasLocalhost9000 = allowedOrigins
                    .stream()
                    .anyMatch(origin -> origin.contains("localhost:9000") || origin.contains("127.0.0.1:9000"));
                boolean hasLocalhost9060 = allowedOrigins
                    .stream()
                    .anyMatch(origin -> origin.contains("localhost:9060") || origin.contains("127.0.0.1:9060"));

                if (!hasLocalhost9000) {
                    allowedOrigins.add("http://localhost:9000");
                    allowedOrigins.add("https://localhost:9000");
                    LOG.info("Auto-added localhost:9000 to CORS allowed origins for local development");
                }
                if (!hasLocalhost9060) {
                    allowedOrigins.add("http://localhost:9060");
                    allowedOrigins.add("https://localhost:9060");
                }
                config.setAllowedOrigins(allowedOrigins);
            }
        } catch (Exception e) {
            // If we can't determine localhost, just continue without auto-adding
            LOG.debug("Could not determine if running on localhost: {}", e.getMessage());
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
            source.registerCorsConfiguration("/services/*/management/**", config);
            source.registerCorsConfiguration("/services/*/v3/api-docs/**", config);
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
}

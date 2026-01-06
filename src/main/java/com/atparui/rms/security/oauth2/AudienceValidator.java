package com.atparui.rms.security.oauth2;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger LOG = LoggerFactory.getLogger(AudienceValidator.class);
    private final OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);

    private final List<String> allowedAudience;

    public AudienceValidator(List<String> allowedAudience) {
        Assert.notEmpty(allowedAudience, "Allowed audience should not be null or empty.");
        this.allowedAudience = allowedAudience;
    }

    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        // Service account tokens may not have an audience claim
        // If audience is null or empty, skip validation for service accounts
        if (audience == null || audience.isEmpty()) {
            LOG.debug("JWT token has no audience claim - allowing token (likely a service account token)");
            return OAuth2TokenValidatorResult.success();
        }

        // Check if any audience matches the allowed list
        boolean matches = audience.stream().anyMatch(allowedAudience::contains);

        // Also accept the client ID as audience (common in OIDC ID tokens)
        // The client ID (gateway-web) is often used as the audience in ID tokens
        if (!matches) {
            // Get client ID from token claims (azp claim or client_id claim)
            String clientId = jwt.getClaimAsString("azp"); // Authorized party
            if (clientId == null) {
                clientId = jwt.getClaimAsString("client_id");
            }
            // If the audience contains the client ID, and client ID is in allowed list, accept it
            if (clientId != null && allowedAudience.contains(clientId)) {
                LOG.debug("Token audience {} contains client ID {} which is in allowed list - accepting", audience, clientId);
                matches = true;
            }
            // Also check if any audience value itself is in the allowed list (e.g., gateway-web)
            if (!matches && audience.stream().anyMatch(aud -> allowedAudience.contains(aud))) {
                matches = true;
            }
        }

        if (matches) {
            LOG.debug("JWT audience validation passed: {} matches allowed audiences: {}", audience, allowedAudience);
            return OAuth2TokenValidatorResult.success();
        } else {
            LOG.warn("Invalid audience: {} (expected one of: {})", audience, allowedAudience);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}

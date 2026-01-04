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
        if (audience.stream().anyMatch(allowedAudience::contains)) {
            return OAuth2TokenValidatorResult.success();
        } else {
            LOG.warn("Invalid audience: {}", audience);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}

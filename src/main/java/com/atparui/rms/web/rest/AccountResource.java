package com.atparui.rms.web.rest;

import com.atparui.rms.service.UserService;
import com.atparui.rms.service.dto.AdminUserDTO;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private static class AccountResourceException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private AccountResourceException(String message) {
            super(message);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

    private final UserService userService;

    public AccountResource(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @param principal the current user; resolves to {@code null} if not authenticated.
     * @return the current user.
     * @throws AccountResourceException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/account")
    public Mono<AdminUserDTO> getAccount(Principal principal) {
        LOG.info("=== Account Endpoint Called ===");
        LOG.info("Principal: {}", principal != null ? principal.getName() : "null");
        LOG.info("Principal class: {}", principal != null ? principal.getClass().getName() : "null");

        // Log authentication type
        if (principal instanceof AbstractAuthenticationToken) {
            AbstractAuthenticationToken authToken = (AbstractAuthenticationToken) principal;
            LOG.info("Authentication type: {}", authToken.getClass().getSimpleName());
            if (authToken.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
                LOG.info("✓ Authenticated via JWT Bearer token (oauth2ResourceServer)");
            } else if (authToken.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser) {
                LOG.info("✓ Authenticated via OAuth2 Login (session-based)");
            }
        }

        if (principal instanceof AbstractAuthenticationToken) {
            AbstractAuthenticationToken authToken = (AbstractAuthenticationToken) principal;
            LOG.info("Authentication authorities: {}", authToken.getAuthorities());
            LOG.info("Authentication name: {}", authToken.getName());
            LOG.info("Authentication details: {}", authToken.getDetails());

            return userService
                .getUserFromAuthentication(authToken)
                .doOnSuccess(user -> LOG.info("Account retrieved successfully for user: {}", user.getLogin()))
                .doOnError(error -> LOG.error("Error retrieving account: {}", error.getMessage(), error));
        } else {
            LOG.warn("Principal is not an AbstractAuthenticationToken, cannot retrieve account");
            throw new AccountResourceException("User could not be found");
        }
    }

    /**
     * {@code GET  /authenticate} : check if the user is authenticated.
     *
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)},
     * or with status {@code 401 (Unauthorized)} if not authenticated.
     */
    @GetMapping("/authenticate")
    public ResponseEntity<Void> isAuthenticated(Principal principal) {
        LOG.debug("REST request to check if the current user is authenticated");
        return ResponseEntity.status(principal == null ? HttpStatus.UNAUTHORIZED : HttpStatus.NO_CONTENT).build();
    }
}

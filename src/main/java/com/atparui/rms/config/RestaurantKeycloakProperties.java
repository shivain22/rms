package com.atparui.rms.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "restaurant.keycloak")
public class RestaurantKeycloakProperties {

    private Theme theme = new Theme();
    private Authentication authentication = new Authentication();
    private Registration registration = new Registration();
    private Security security = new Security();

    public static class Theme {

        private String loginTheme = "restaurant-theme";
        private String accountTheme = "restaurant-theme";
        private String adminTheme = "keycloak";
        private String emailTheme = "restaurant-theme";

        // Getters and setters
        public String getLoginTheme() {
            return loginTheme;
        }

        public void setLoginTheme(String loginTheme) {
            this.loginTheme = loginTheme;
        }

        public String getAccountTheme() {
            return accountTheme;
        }

        public void setAccountTheme(String accountTheme) {
            this.accountTheme = accountTheme;
        }

        public String getAdminTheme() {
            return adminTheme;
        }

        public void setAdminTheme(String adminTheme) {
            this.adminTheme = adminTheme;
        }

        public String getEmailTheme() {
            return emailTheme;
        }

        public void setEmailTheme(String emailTheme) {
            this.emailTheme = emailTheme;
        }
    }

    public static class Authentication {

        private String browserFlow = "restaurant-browser-flow";
        private String registrationFlow = "restaurant-registration-flow";
        private List<String> requiredActions = List.of("VERIFY_EMAIL", "UPDATE_PASSWORD");
        private boolean rememberMe = true;
        private boolean resetPasswordAllowed = true;

        // Getters and setters
        public String getBrowserFlow() {
            return browserFlow;
        }

        public void setBrowserFlow(String browserFlow) {
            this.browserFlow = browserFlow;
        }

        public String getRegistrationFlow() {
            return registrationFlow;
        }

        public void setRegistrationFlow(String registrationFlow) {
            this.registrationFlow = registrationFlow;
        }

        public List<String> getRequiredActions() {
            return requiredActions;
        }

        public void setRequiredActions(List<String> requiredActions) {
            this.requiredActions = requiredActions;
        }

        public boolean isRememberMe() {
            return rememberMe;
        }

        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
        }

        public boolean isResetPasswordAllowed() {
            return resetPasswordAllowed;
        }

        public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
            this.resetPasswordAllowed = resetPasswordAllowed;
        }
    }

    public static class Registration {

        private boolean enabled = true;
        private boolean emailAsUsername = true;
        private boolean verifyEmail = true;
        private List<String> defaultRoles = List.of("ROLE_CUSTOMER");

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEmailAsUsername() {
            return emailAsUsername;
        }

        public void setEmailAsUsername(boolean emailAsUsername) {
            this.emailAsUsername = emailAsUsername;
        }

        public boolean isVerifyEmail() {
            return verifyEmail;
        }

        public void setVerifyEmail(boolean verifyEmail) {
            this.verifyEmail = verifyEmail;
        }

        public List<String> getDefaultRoles() {
            return defaultRoles;
        }

        public void setDefaultRoles(List<String> defaultRoles) {
            this.defaultRoles = defaultRoles;
        }
    }

    public static class Security {

        private int accessTokenLifespan = 900; // 15 minutes
        private int refreshTokenMaxReuse = 0;
        private int ssoSessionIdleTimeout = 1800; // 30 minutes
        private int ssoSessionMaxLifespan = 36000; // 10 hours
        private PasswordPolicy passwordPolicy = new PasswordPolicy();

        public static class PasswordPolicy {

            private int minLength = 8;
            private int minDigits = 1;
            private int minLowerCase = 1;
            private int minUpperCase = 1;
            private int minSpecialChars = 1;
            private boolean notUsername = true;
            private int passwordHistory = 3;

            // Getters and setters
            public int getMinLength() {
                return minLength;
            }

            public void setMinLength(int minLength) {
                this.minLength = minLength;
            }

            public int getMinDigits() {
                return minDigits;
            }

            public void setMinDigits(int minDigits) {
                this.minDigits = minDigits;
            }

            public int getMinLowerCase() {
                return minLowerCase;
            }

            public void setMinLowerCase(int minLowerCase) {
                this.minLowerCase = minLowerCase;
            }

            public int getMinUpperCase() {
                return minUpperCase;
            }

            public void setMinUpperCase(int minUpperCase) {
                this.minUpperCase = minUpperCase;
            }

            public int getMinSpecialChars() {
                return minSpecialChars;
            }

            public void setMinSpecialChars(int minSpecialChars) {
                this.minSpecialChars = minSpecialChars;
            }

            public boolean isNotUsername() {
                return notUsername;
            }

            public void setNotUsername(boolean notUsername) {
                this.notUsername = notUsername;
            }

            public int getPasswordHistory() {
                return passwordHistory;
            }

            public void setPasswordHistory(int passwordHistory) {
                this.passwordHistory = passwordHistory;
            }
        }

        // Getters and setters
        public int getAccessTokenLifespan() {
            return accessTokenLifespan;
        }

        public void setAccessTokenLifespan(int accessTokenLifespan) {
            this.accessTokenLifespan = accessTokenLifespan;
        }

        public int getRefreshTokenMaxReuse() {
            return refreshTokenMaxReuse;
        }

        public void setRefreshTokenMaxReuse(int refreshTokenMaxReuse) {
            this.refreshTokenMaxReuse = refreshTokenMaxReuse;
        }

        public int getSsoSessionIdleTimeout() {
            return ssoSessionIdleTimeout;
        }

        public void setSsoSessionIdleTimeout(int ssoSessionIdleTimeout) {
            this.ssoSessionIdleTimeout = ssoSessionIdleTimeout;
        }

        public int getSsoSessionMaxLifespan() {
            return ssoSessionMaxLifespan;
        }

        public void setSsoSessionMaxLifespan(int ssoSessionMaxLifespan) {
            this.ssoSessionMaxLifespan = ssoSessionMaxLifespan;
        }

        public PasswordPolicy getPasswordPolicy() {
            return passwordPolicy;
        }

        public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
            this.passwordPolicy = passwordPolicy;
        }
    }

    // Main getters and setters
    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }
}

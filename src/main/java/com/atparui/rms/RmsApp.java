package com.atparui.rms;

import com.atparui.rms.config.ApplicationProperties;
import com.atparui.rms.config.CRLFLogConverter;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import tech.jhipster.config.DefaultProfileUtil;
import tech.jhipster.config.JHipsterConstants;

@SpringBootApplication
@EnableConfigurationProperties({ LiquibaseProperties.class, ApplicationProperties.class })
public class RmsApp {

    private static final Logger LOG = LoggerFactory.getLogger(RmsApp.class);

    private final Environment env;

    public RmsApp(Environment env) {
        this.env = env;
    }

    /**
     * Initializes rms.
     * <p>
     * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
     * <p>
     * You can find more information on how profiles work with JHipster on <a href="https://www.jhipster.tech/profiles/">https://www.jhipster.tech/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
        ) {
            LOG.error(
                "You have misconfigured your application! It should not run " + "with both the 'dev' and 'prod' profiles at the same time."
            );
        }
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_CLOUD)
        ) {
            LOG.error(
                "You have misconfigured your application! It should not " + "run with both the 'dev' and 'cloud' profiles at the same time."
            );
        }
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        // Set JVM timezone to UTC to avoid PostgreSQL timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication app = new SpringApplication(RmsApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store")).map(key -> "https").orElse("http");
        String applicationName = env.getProperty("spring.application.name");
        String serverPort = env.getProperty("server.port");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path"))
            .filter(StringUtils::isNotBlank)
            .orElse("/");
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOG.warn("The host name could not be determined, using `localhost` as fallback");
        }
        // Get build version information
        String buildVersion = env.getProperty("jhipster.build.short-version", env.getProperty("jhipster.api-docs.version", "0.0.1"));
        String buildCommitHash = env.getProperty("jhipster.build.commit-hash", "unknown");
        String buildCommitCount = env.getProperty("jhipster.build.commit-count", "0");
        String buildBranch = env.getProperty("jhipster.build.branch", "unknown");
        String buildTimestamp = env.getProperty("jhipster.build.timestamp", "unknown");

        LOG.info(
            CRLFLogConverter.CRLF_SAFE_MARKER,
            """

            ----------------------------------------------------------
            \tApplication '{}' is running! Access URLs:
            \tLocal: \t\t{}://localhost:{}{}
            \tExternal: \t{}://{}:{}{}
            \tProfile(s): \t{}
            \tVersion: \t{} (commit: {}, count: {}, branch: {})
            \tBuild Time: \t{}
            ----------------------------------------------------------""",
            applicationName,
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles(),
            buildVersion,
            buildCommitHash,
            buildCommitCount,
            buildBranch,
            buildTimestamp
        );

        String configServerStatus = env.getProperty("configserver.status");
        if (configServerStatus == null) {
            // Check if Consul Config is enabled
            String consulConfigEnabled = env.getProperty("spring.cloud.consul.config.enabled");
            String consulHost = env.getProperty("spring.cloud.consul.host", "localhost");
            String consulPort = env.getProperty("spring.cloud.consul.port", "8500");
            if ("true".equalsIgnoreCase(consulConfigEnabled)) {
                configServerStatus = String.format("Consul Config (enabled) - %s:%s", consulHost, consulPort);
            } else {
                configServerStatus = "Not found or not setup for this application";
            }
        }
        LOG.info(
            CRLFLogConverter.CRLF_SAFE_MARKER,
            "\n----------------------------------------------------------\n\t" +
            "Config Server: \t{}\n----------------------------------------------------------",
            configServerStatus
        );
    }
}

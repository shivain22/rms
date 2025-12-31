package com.atparui.rms.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SSH Tunnel Configuration for local development.
 * Creates multiple SSH tunnels to remote services when running locally.
 *
 * This is only active when:
 * - spring.profiles.active includes 'dev' or 'local'
 * - ssh.tunnel.enabled=true
 *
 * Supports tunneling for:
 * - PostgreSQL (rms-postgresql)
 * - Kafka
 * - Consul
 * - Elasticsearch
 * - Keycloak
 */
@Configuration
@Profile({ "dev", "local" })
@ConditionalOnProperty(name = "ssh.tunnel.enabled", havingValue = "true", matchIfMissing = false)
public class SshTunnelConfig {

    private static final Logger log = LoggerFactory.getLogger(SshTunnelConfig.class);

    // SSH Connection Settings
    @Value("${ssh.tunnel.host:}")
    private String sshHost;

    @Value("${ssh.tunnel.port:22}")
    private int sshPort;

    @Value("${ssh.tunnel.username:}")
    private String sshUsername;

    @Value("${ssh.tunnel.password:}")
    private String sshPassword;

    @Value("${ssh.tunnel.private-key:}")
    private String sshPrivateKey;

    @Value("${ssh.tunnel.private-key-passphrase:}")
    private String sshPrivateKeyPassphrase;

    // PostgreSQL Tunnel Settings
    @Value("${ssh.tunnel.postgres.remote-host:localhost}")
    private String postgresRemoteHost;

    @Value("${ssh.tunnel.postgres.remote-port:5435}")
    private int postgresRemotePort;

    @Value("${ssh.tunnel.postgres.local-port:5433}")
    private int postgresLocalPort;

    // Kafka Tunnel Settings
    @Value("${ssh.tunnel.kafka.remote-host:localhost}")
    private String kafkaRemoteHost;

    @Value("${ssh.tunnel.kafka.remote-port:9295}")
    private int kafkaRemotePort;

    @Value("${ssh.tunnel.kafka.local-port:9092}")
    private int kafkaLocalPort;

    // Consul Tunnel Settings
    @Value("${ssh.tunnel.consul.remote-host:localhost}")
    private String consulRemoteHost;

    @Value("${ssh.tunnel.consul.remote-port:8500}")
    private int consulRemotePort;

    @Value("${ssh.tunnel.consul.local-port:8500}")
    private int consulLocalPort;

    // Elasticsearch Tunnel Settings
    @Value("${ssh.tunnel.elasticsearch.remote-host:localhost}")
    private String elasticsearchRemoteHost;

    @Value("${ssh.tunnel.elasticsearch.remote-port:9200}")
    private int elasticsearchRemotePort;

    @Value("${ssh.tunnel.elasticsearch.local-port:9200}")
    private int elasticsearchLocalPort;

    // Keycloak Tunnel Settings
    @Value("${ssh.tunnel.keycloak.remote-host:localhost}")
    private String keycloakRemoteHost;

    @Value("${ssh.tunnel.keycloak.remote-port:9292}")
    private int keycloakRemotePort;

    @Value("${ssh.tunnel.keycloak.local-port:8080}")
    private int keycloakLocalPort;

    private Session session;
    private JSch jsch;
    private final List<TunnelInfo> tunnels = new ArrayList<>();

    @PostConstruct
    public void createTunnels() {
        if (!isEnabled()) {
            log.info("SSH tunnel is disabled, skipping tunnel creation");
            return;
        }

        log.info("=== Creating SSH Tunnels ===");
        log.info("SSH Host: {}:{}", sshHost, sshPort);
        log.info("SSH Username: {}", sshUsername);

        try {
            jsch = new JSch();

            // Use private key if provided, otherwise use password
            if (sshPrivateKey != null && !sshPrivateKey.isEmpty()) {
                if (sshPrivateKeyPassphrase != null && !sshPrivateKeyPassphrase.isEmpty()) {
                    jsch.addIdentity(sshPrivateKey, sshPrivateKeyPassphrase);
                } else {
                    jsch.addIdentity(sshPrivateKey);
                }
                log.info("Using SSH private key authentication");
            } else {
                log.info("Using SSH password authentication");
            }

            session = jsch.getSession(sshUsername, sshHost, sshPort);
            session.setConfig("StrictHostKeyChecking", "no");

            if (sshPassword != null && !sshPassword.isEmpty()) {
                session.setPassword(sshPassword);
            }

            session.connect(30000); // 30 second timeout
            log.info("SSH session connected successfully");

            // Create PostgreSQL tunnel
            createTunnel("PostgreSQL", postgresLocalPort, postgresRemoteHost, postgresRemotePort);

            // Create Kafka tunnel
            createTunnel("Kafka", kafkaLocalPort, kafkaRemoteHost, kafkaRemotePort);

            // Create Consul tunnel
            createTunnel("Consul", consulLocalPort, consulRemoteHost, consulRemotePort);

            // Create Elasticsearch tunnel
            createTunnel("Elasticsearch", elasticsearchLocalPort, elasticsearchRemoteHost, elasticsearchRemotePort);

            // Create Keycloak tunnel
            createTunnel("Keycloak", keycloakLocalPort, keycloakRemoteHost, keycloakRemotePort);

            log.info("=== All SSH Tunnels Established ===");
            tunnels.forEach(tunnel ->
                log.info("  {}: localhost:{} -> {}:{}:{}", tunnel.name, tunnel.localPort, sshHost, tunnel.remoteHost, tunnel.remotePort)
            );
            log.info("=========================================");
        } catch (JSchException e) {
            log.error("Failed to create SSH tunnels: {}", e.getMessage(), e);
            throw new RuntimeException("SSH tunnel creation failed", e);
        }
    }

    private void createTunnel(String name, int localPort, String remoteHost, int remotePort) throws JSchException {
        int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);
        tunnels.add(new TunnelInfo(name, assignedPort, remoteHost, remotePort));
        log.info("{} tunnel established: localhost:{} -> {}:{}:{}", name, assignedPort, sshHost, remoteHost, remotePort);
    }

    @PreDestroy
    public void closeTunnels() {
        if (session != null && session.isConnected()) {
            log.info("Closing SSH tunnels...");
            session.disconnect();
            log.info("All SSH tunnels closed");
        }
    }

    private boolean isEnabled() {
        return sshHost != null && !sshHost.isEmpty() && sshUsername != null && !sshUsername.isEmpty();
    }

    /**
     * Get the local port for PostgreSQL tunnel.
     */
    public int getPostgresLocalPort() {
        return postgresLocalPort;
    }

    /**
     * Get the local port for Kafka tunnel.
     */
    public int getKafkaLocalPort() {
        return kafkaLocalPort;
    }

    /**
     * Get the local port for Consul tunnel.
     */
    public int getConsulLocalPort() {
        return consulLocalPort;
    }

    /**
     * Get the local port for Elasticsearch tunnel.
     */
    public int getElasticsearchLocalPort() {
        return elasticsearchLocalPort;
    }

    /**
     * Get the local port for Keycloak tunnel.
     */
    public int getKeycloakLocalPort() {
        return keycloakLocalPort;
    }

    private static class TunnelInfo {

        final String name;
        final int localPort;
        final String remoteHost;
        final int remotePort;

        TunnelInfo(String name, int localPort, String remoteHost, int remotePort) {
            this.name = name;
            this.localPort = localPort;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
        }
    }
}

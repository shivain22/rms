package com.atparui.rms.web.rest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for viewing and managing Logback configuration at runtime.
 */
@RestController
@RequestMapping("/api/admin/loggers")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class LoggersResource {

    private static final Logger LOG = LoggerFactory.getLogger(LoggersResource.class);

    private final LoggerContext loggerContext;

    public LoggersResource() {
        this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        LOG.info("LoggersResource initialized - LoggerContext: {}", loggerContext);
    }

    /**
     * {@code GET  /loggers} : get the list of all loggers.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of loggers in body.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<LoggerVM>> getLoggers() {
        LOG.info("=== Loggers Endpoint Called ===");
        LOG.info("Getting all loggers from LoggerContext");
        List<LoggerVM> loggers = loggerContext.getLoggerList().stream().map(LoggerVM::new).collect(Collectors.toList());
        LOG.info("Found {} loggers", loggers.size());
        return Mono.just(loggers);
    }

    /**
     * {@code GET  /loggers/:name} : get a specific logger by name.
     *
     * @param name the name of the logger to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the logger in body, or with status {@code 404 (Not Found)}.
     */
    @GetMapping(value = "/{name:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<LoggerVM> getLogger(@PathVariable String name) {
        return Mono.just(new LoggerVM(loggerContext.getLogger(name)));
    }

    /**
     * {@code PUT  /loggers/:name} : change the level of a logger.
     *
     * @param name the name of the logger to update.
     * @param jsonLogger the logger configuration.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @PutMapping(value = "/{name:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Void> changeLevel(@PathVariable String name, @RequestBody LoggerVM jsonLogger) {
        loggerContext.getLogger(name).setLevel(Level.valueOf(jsonLogger.getLevel()));
        return Mono.empty();
    }

    /**
     * View Model object for storing a Logger's information.
     */
    public static class LoggerVM {

        private String name;

        private String level;

        public LoggerVM() {
            // Empty constructor needed for Jackson.
        }

        public LoggerVM(ch.qos.logback.classic.Logger logger) {
            this.name = logger.getName();
            this.level = logger.getEffectiveLevel().toString();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }
}

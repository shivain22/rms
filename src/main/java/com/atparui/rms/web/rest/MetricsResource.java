package com.atparui.rms.web.rest;

import io.micrometer.core.instrument.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for exposing JHipster metrics.
 */
@RestController
@RequestMapping("/management/jhimetrics")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class MetricsResource {

    private final MeterRegistry meterRegistry;

    public MetricsResource(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * {@code GET  /jhimetrics} : get the metrics.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the metrics in body.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM Metrics
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("memory", getJvmMemoryMetrics());
        metrics.put("jvm", jvm);

        // Process Metrics
        metrics.put("processMetrics", getProcessMetrics());

        // Garbage Collector Metrics
        metrics.put("garbageCollector", getGarbageCollectorMetrics());

        // HTTP Server Requests
        metrics.put("http.server.requests", getHttpServerRequestsMetrics());

        // Services/Endpoints Metrics
        metrics.put("services", getServicesMetrics());

        // Cache Metrics
        metrics.put("cache", getCacheMetrics());

        // Database Metrics
        metrics.put("databases", getDatabaseMetrics());

        return Mono.just(metrics);
    }

    private Map<String, Object> getJvmMemoryMetrics() {
        Map<String, Object> memory = new HashMap<>();

        // Heap memory
        Map<String, Object> heap = new HashMap<>();
        heap.put("committed", getMetricValue("jvm.memory.used", "area", "heap"));
        heap.put("init", getMetricValue("jvm.memory.committed", "area", "heap"));
        heap.put("max", getMetricValue("jvm.memory.max", "area", "heap"));
        heap.put("used", getMetricValue("jvm.memory.used", "area", "heap"));
        memory.put("heap", heap);

        // Non-heap memory
        Map<String, Object> nonheap = new HashMap<>();
        nonheap.put("committed", getMetricValue("jvm.memory.committed", "area", "nonheap"));
        nonheap.put("init", getMetricValue("jvm.memory.committed", "area", "nonheap"));
        nonheap.put("max", getMetricValue("jvm.memory.max", "area", "nonheap"));
        nonheap.put("used", getMetricValue("jvm.memory.used", "area", "nonheap"));
        memory.put("nonheap", nonheap);

        return memory;
    }

    private Map<String, Object> getProcessMetrics() {
        Map<String, Object> process = new HashMap<>();
        process.put("uptime", getMetricValue("process.uptime"));
        process.put("cpuUsage", getMetricValue("process.cpu.usage"));
        process.put("openFileDescriptors", getMetricValue("process.files.open"));
        process.put("maxFileDescriptors", getMetricValue("process.files.max"));
        return process;
    }

    private Map<String, Object> getGarbageCollectorMetrics() {
        Map<String, Object> gc = new HashMap<>();

        // Get all GC metrics
        List<String> gcNames = meterRegistry
            .find("jvm.gc.pause")
            .meters()
            .stream()
            .map(m -> m.getId().getTag("action"))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        for (String gcName : gcNames) {
            Map<String, Object> gcStats = new HashMap<>();
            gcStats.put("count", getMetricValue("jvm.gc.pause", "action", gcName, Statistic.COUNT));
            gcStats.put("totalTime", getMetricValue("jvm.gc.pause", "action", gcName, Statistic.TOTAL_TIME));
            gc.put(gcName, gcStats);
        }

        return gc;
    }

    private Map<String, Object> getHttpServerRequestsMetrics() {
        Map<String, Object> httpMetrics = new HashMap<>();

        // Get HTTP request metrics by status
        List<String> statuses = meterRegistry
            .find("http.server.requests")
            .meters()
            .stream()
            .map(m -> m.getId().getTag("status"))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        for (String status : statuses) {
            Map<String, Object> statusMetrics = new HashMap<>();
            statusMetrics.put("count", getMetricValue("http.server.requests", "status", status, Statistic.COUNT));
            statusMetrics.put("totalTime", getMetricValue("http.server.requests", "status", status, Statistic.TOTAL_TIME));
            httpMetrics.put(status, statusMetrics);
        }

        return httpMetrics;
    }

    private Map<String, Object> getServicesMetrics() {
        Map<String, Object> services = new HashMap<>();

        // Get metrics by endpoint
        Map<String, Map<String, Object>> endpoints = new HashMap<>();
        meterRegistry
            .find("http.server.requests")
            .meters()
            .forEach(meter -> {
                String uri = meter.getId().getTag("uri");
                if (uri != null && !uri.equals("UNKNOWN")) {
                    endpoints
                        .computeIfAbsent(uri, k -> new HashMap<>())
                        .put("count", getMetricValue("http.server.requests", "uri", uri, Statistic.COUNT));
                }
            });

        services.put("endpoints", endpoints);
        return services;
    }

    private Map<String, Object> getCacheMetrics() {
        Map<String, Object> cache = new HashMap<>();

        // Get cache metrics
        meterRegistry
            .find("cache")
            .meters()
            .forEach(meter -> {
                String cacheName = meter.getId().getTag("name");
                String cacheResult = meter.getId().getTag("result");
                if (cacheName != null && cacheResult != null) {
                    Map<String, Object> cacheMap = (Map<String, Object>) cache.computeIfAbsent(cacheName, k -> new HashMap<>());
                    cacheMap.put(cacheResult, getMetricValue("cache." + cacheResult, "name", cacheName));
                }
            });

        return cache;
    }

    private Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> databases = new HashMap<>();

        // Get database connection pool metrics
        meterRegistry
            .find("jdbc.connections")
            .meters()
            .forEach(meter -> {
                String pool = meter.getId().getTag("pool");
                String state = meter.getId().getTag("state");
                if (pool != null && state != null) {
                    Map<String, Object> dbMap = (Map<String, Object>) databases.computeIfAbsent(pool, k -> new HashMap<>());
                    dbMap.put(state, getMetricValue("jdbc.connections", "pool", pool, "state", state));
                }
            });

        return databases;
    }

    private Double getMetricValue(String metricName) {
        try {
            Meter meter = meterRegistry.find(metricName).meter();
            if (meter != null) {
                return getMeterValue(meter);
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMetricValue(String metricName, String tagKey, String tagValue) {
        try {
            Meter meter = meterRegistry.find(metricName).tag(tagKey, tagValue).meter();
            if (meter != null) {
                return getMeterValue(meter);
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMetricValue(String metricName, String tagKey1, String tagValue1, String tagKey2, String tagValue2) {
        try {
            Meter meter = meterRegistry.find(metricName).tag(tagKey1, tagValue1).tag(tagKey2, tagValue2).meter();
            if (meter != null) {
                return getMeterValue(meter);
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMetricValue(String metricName, String tagKey, String tagValue, Statistic statistic) {
        try {
            Iterable<Measurement> measurements = meterRegistry.find(metricName).tag(tagKey, tagValue).meter().measure();
            if (measurements != null) {
                for (Measurement m : measurements) {
                    if (m.getStatistic() == statistic) {
                        return m.getValue();
                    }
                }
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMetricValue(
        String metricName,
        String tagKey1,
        String tagValue1,
        String tagKey2,
        String tagValue2,
        Statistic statistic
    ) {
        try {
            Iterable<Measurement> measurements = meterRegistry
                .find(metricName)
                .tag(tagKey1, tagValue1)
                .tag(tagKey2, tagValue2)
                .meter()
                .measure();
            if (measurements != null) {
                for (Measurement m : measurements) {
                    if (m.getStatistic() == statistic) {
                        return m.getValue();
                    }
                }
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMeterValue(Meter meter) {
        try {
            Iterable<Measurement> measurements = meter.measure();
            if (measurements != null) {
                for (Measurement m : measurements) {
                    return m.getValue();
                }
            }
        } catch (Exception e) {
            // Error getting value
        }
        return 0.0;
    }
}

package com.atparui.rms.web.rest;

import io.micrometer.core.instrument.*;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

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
        try {
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
            Map<String, Object> httpMetrics = getHttpServerRequestsMetrics();
            metrics.put("http.server.requests", httpMetrics);

            // Services/Endpoints Metrics
            metrics.put("services", getServicesMetrics());

            // Cache Metrics
            metrics.put("cache", getCacheMetrics());

            // Database Metrics
            metrics.put("databases", getDatabaseMetrics());

            LOG.debug("Returning metrics: {}", metrics.keySet());
            return Mono.just(metrics);
        } catch (Exception e) {
            LOG.error("Error getting metrics", e);
            // Return empty metrics structure to prevent frontend errors
            Map<String, Object> emptyMetrics = new HashMap<>();
            emptyMetrics.put("jvm", new HashMap<>());
            emptyMetrics.put("processMetrics", new HashMap<>());
            emptyMetrics.put("garbageCollector", new HashMap<>());
            emptyMetrics.put("http.server.requests", new HashMap<>());
            emptyMetrics.put("services", new HashMap<>());
            emptyMetrics.put("cache", new HashMap<>());
            emptyMetrics.put("databases", new HashMap<>());
            return Mono.just(emptyMetrics);
        }
    }

    private Map<String, Object> getJvmMemoryMetrics() {
        Map<String, Object> memory = new HashMap<>();

        // Heap memory - get all heap meters
        Map<String, Object> heap = new HashMap<>();
        Collection<Meter> heapMeters = meterRegistry.find("jvm.memory.used").tag("area", "heap").meters();
        if (heapMeters != null && !heapMeters.isEmpty()) {
            for (Meter meter : heapMeters) {
                Iterable<Measurement> measurements = meter.measure();
                if (measurements != null) {
                    for (Measurement m : measurements) {
                        if (m.getStatistic() == Statistic.VALUE) {
                            heap.put("used", m.getValue());
                            break;
                        }
                    }
                }
            }
        }
        heap.put("committed", getMetricValue("jvm.memory.committed", "area", "heap"));
        heap.put("init", getMetricValue("jvm.memory.init", "area", "heap"));
        heap.put("max", getMetricValue("jvm.memory.max", "area", "heap"));
        if (!heap.containsKey("used")) {
            heap.put("used", getMetricValue("jvm.memory.used", "area", "heap"));
        }
        memory.put("heap", heap);

        // Non-heap memory
        Map<String, Object> nonheap = new HashMap<>();
        nonheap.put("committed", getMetricValue("jvm.memory.committed", "area", "nonheap"));
        nonheap.put("init", getMetricValue("jvm.memory.init", "area", "nonheap"));
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

        // Get all HTTP server request meters
        Collection<Meter> meters = meterRegistry.find("http.server.requests").meters();

        if (meters == null || meters.isEmpty()) {
            return httpMetrics;
        }

        // Group by status code and aggregate values
        Map<String, Map<String, Double>> statusMap = new HashMap<>();

        for (Meter meter : meters) {
            String status = meter.getId().getTag("status");
            if (status != null) {
                Map<String, Double> statusMetrics = statusMap.computeIfAbsent(status, k -> new HashMap<>());

                // Get measurements for this meter and aggregate
                Iterable<Measurement> measurements = meter.measure();
                if (measurements != null) {
                    for (Measurement measurement : measurements) {
                        Statistic stat = measurement.getStatistic();
                        Double value = measurement.getValue();

                        if (stat == Statistic.COUNT) {
                            statusMetrics.put("count", statusMetrics.getOrDefault("count", 0.0) + value);
                        } else if (stat == Statistic.TOTAL_TIME) {
                            statusMetrics.put("totalTime", statusMetrics.getOrDefault("totalTime", 0.0) + value);
                        } else if (stat == Statistic.MAX) {
                            // For MAX, we want the maximum value, not sum
                            Double currentMax = statusMetrics.get("max");
                            if (currentMax == null || value > currentMax) {
                                statusMetrics.put("max", value);
                            }
                        }
                    }
                }
            }
        }

        // Convert to expected format - ensure all required properties exist
        for (Map.Entry<String, Map<String, Double>> entry : statusMap.entrySet()) {
            Map<String, Object> statusMetrics = new HashMap<>();
            Map<String, Double> values = entry.getValue();
            // Always include count and totalTime, defaulting to 0.0 if not present
            statusMetrics.put("count", values.getOrDefault("count", 0.0));
            statusMetrics.put("totalTime", values.getOrDefault("totalTime", 0.0));
            if (values.containsKey("max")) {
                statusMetrics.put("max", values.get("max"));
            }
            httpMetrics.put(entry.getKey(), statusMetrics);
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
            Collection<Meter> meters = meterRegistry.find(metricName).meters();
            if (meters != null && !meters.isEmpty()) {
                for (Meter meter : meters) {
                    Double value = getMeterValue(meter);
                    if (value != null && value != 0.0) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMetricValue(String metricName, String tagKey, String tagValue) {
        try {
            Collection<Meter> meters = meterRegistry.find(metricName).tag(tagKey, tagValue).meters();
            if (meters != null && !meters.isEmpty()) {
                for (Meter meter : meters) {
                    Double value = getMeterValue(meter);
                    if (value != null && value != 0.0) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMetricValue(String metricName, String tagKey1, String tagValue1, String tagKey2, String tagValue2) {
        try {
            Collection<Meter> meters = meterRegistry.find(metricName).tag(tagKey1, tagValue1).tag(tagKey2, tagValue2).meters();
            if (meters != null && !meters.isEmpty()) {
                for (Meter meter : meters) {
                    Double value = getMeterValue(meter);
                    if (value != null && value != 0.0) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            // Metric not found or not available
        }
        return 0.0;
    }

    private Double getMetricValue(String metricName, String tagKey, String tagValue, Statistic statistic) {
        try {
            Collection<Meter> meters = meterRegistry.find(metricName).tag(tagKey, tagValue).meters();
            if (meters != null && !meters.isEmpty()) {
                for (Meter meter : meters) {
                    Iterable<Measurement> measurements = meter.measure();
                    if (measurements != null) {
                        for (Measurement m : measurements) {
                            if (m.getStatistic() == statistic) {
                                return m.getValue();
                            }
                        }
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
            Collection<Meter> meters = meterRegistry.find(metricName).tag(tagKey1, tagValue1).tag(tagKey2, tagValue2).meters();
            if (meters != null && !meters.isEmpty()) {
                for (Meter meter : meters) {
                    Iterable<Measurement> measurements = meter.measure();
                    if (measurements != null) {
                        for (Measurement m : measurements) {
                            if (m.getStatistic() == statistic) {
                                return m.getValue();
                            }
                        }
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

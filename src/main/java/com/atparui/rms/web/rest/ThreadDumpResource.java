package com.atparui.rms.web.rest;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for exposing thread dump information.
 */
@RestController
@RequestMapping("/management/threaddump")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class ThreadDumpResource {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadDumpResource.class);

    /**
     * {@code GET  /threaddump} : get the thread dump.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the thread dump in body.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Map<String, Object>>> getThreadDump() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadBean.dumpAllThreads(true, true);

            List<Map<String, Object>> threads = new ArrayList<>();

            for (ThreadInfo threadInfo : threadInfos) {
                Map<String, Object> thread = new HashMap<>();
                thread.put("threadName", threadInfo.getThreadName());
                thread.put("threadId", threadInfo.getThreadId());
                thread.put("blockedTime", threadInfo.getBlockedTime());
                thread.put("blockedCount", threadInfo.getBlockedCount());
                thread.put("waitedTime", threadInfo.getWaitedTime());
                thread.put("waitedCount", threadInfo.getWaitedCount());
                thread.put("lockName", threadInfo.getLockName());
                thread.put("lockOwnerId", threadInfo.getLockOwnerId());
                thread.put("lockOwnerName", threadInfo.getLockOwnerName());
                thread.put("inNative", threadInfo.isInNative());
                thread.put("suspended", threadInfo.isSuspended());
                thread.put("threadState", threadInfo.getThreadState().toString());

                // Stack trace
                StackTraceElement[] stackTrace = threadInfo.getStackTrace();
                List<Map<String, Object>> stackTraceList = new ArrayList<>();
                for (StackTraceElement element : stackTrace) {
                    Map<String, Object> stackElement = new HashMap<>();
                    stackElement.put("className", element.getClassName());
                    stackElement.put("fileName", element.getFileName());
                    stackElement.put("lineNumber", element.getLineNumber());
                    stackElement.put("methodName", element.getMethodName());
                    stackElement.put("nativeMethod", element.isNativeMethod());
                    stackTraceList.add(stackElement);
                }
                thread.put("stackTrace", stackTraceList);

                // Locked monitors
                MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
                if (lockedMonitors != null && lockedMonitors.length > 0) {
                    List<Map<String, Object>> lockedMonitorsList = new ArrayList<>();
                    for (MonitorInfo monitor : lockedMonitors) {
                        Map<String, Object> monitorInfo = new HashMap<>();
                        monitorInfo.put("className", monitor.getClassName());
                        monitorInfo.put("identityHashCode", monitor.getIdentityHashCode());
                        monitorInfo.put("lockedStackDepth", monitor.getLockedStackDepth());
                        StackTraceElement lockedFrame = monitor.getLockedStackFrame();
                        if (lockedFrame != null) {
                            Map<String, Object> frame = new HashMap<>();
                            frame.put("className", lockedFrame.getClassName());
                            frame.put("fileName", lockedFrame.getFileName());
                            frame.put("lineNumber", lockedFrame.getLineNumber());
                            frame.put("methodName", lockedFrame.getMethodName());
                            monitorInfo.put("lockedStackFrame", frame);
                        }
                        lockedMonitorsList.add(monitorInfo);
                    }
                    thread.put("lockedMonitors", lockedMonitorsList);
                }

                // Locked synchronizers
                java.lang.management.LockInfo[] lockedSynchronizers = threadInfo.getLockedSynchronizers();
                if (lockedSynchronizers != null && lockedSynchronizers.length > 0) {
                    List<Map<String, Object>> lockedSynchronizersList = new ArrayList<>();
                    for (java.lang.management.LockInfo synchronizer : lockedSynchronizers) {
                        Map<String, Object> syncInfo = new HashMap<>();
                        syncInfo.put("className", synchronizer.getClassName());
                        syncInfo.put("identityHashCode", synchronizer.getIdentityHashCode());
                        lockedSynchronizersList.add(syncInfo);
                    }
                    thread.put("lockedSynchronizers", lockedSynchronizersList);
                }

                threads.add(thread);
            }

            LOG.debug("Returning thread dump with {} threads", threads.size());
            return Mono.just(threads);
        } catch (Exception e) {
            LOG.error("Error getting thread dump", e);
            // Return empty list to prevent frontend errors
            return Mono.just(new ArrayList<>());
        }
    }
}

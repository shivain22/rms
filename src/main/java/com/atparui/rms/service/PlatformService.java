package com.atparui.rms.service;

import com.atparui.rms.domain.Platform;
import com.atparui.rms.repository.PlatformRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PlatformService {

    private static final Logger log = LoggerFactory.getLogger(PlatformService.class);

    private final PlatformRepository platformRepository;

    public PlatformService(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    public Flux<Platform> findAll() {
        return platformRepository.findAll();
    }

    public Flux<Platform> findAllActive() {
        return platformRepository.findAllActive();
    }

    public Mono<Platform> findById(Long id) {
        return platformRepository.findById(id);
    }

    public Mono<Platform> save(Platform platform) {
        if (platform.getId() == null) {
            log.debug("Creating new platform: {}", platform.getName());
        } else {
            log.debug("Updating platform: {}", platform.getId());
        }
        return platformRepository.save(platform);
    }

    public Mono<Void> delete(Long id) {
        log.debug("Deleting platform: {}", id);
        return platformRepository.deleteById(id);
    }
}

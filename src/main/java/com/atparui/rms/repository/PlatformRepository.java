package com.atparui.rms.repository;

import com.atparui.rms.domain.Platform;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PlatformRepository {

    private final R2dbcEntityTemplate masterTemplate;

    public PlatformRepository(@Qualifier("masterR2dbcTemplate") R2dbcEntityTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    public Flux<Platform> findAll() {
        return masterTemplate.select(Platform.class).all();
    }

    public Flux<Platform> findAllActive() {
        return masterTemplate.select(Platform.class).matching(Query.query(Criteria.where("active").is(true))).all();
    }

    public Flux<Platform> findByActiveTrue() {
        return masterTemplate.select(Platform.class).matching(Query.query(Criteria.where("active").is(true))).all();
    }

    public Mono<Platform> findByPrefix(String prefix) {
        return masterTemplate.selectOne(Query.query(Criteria.where("prefix").is(prefix)), Platform.class);
    }

    public Mono<Platform> findById(Long id) {
        return masterTemplate.selectOne(Query.query(Criteria.where("id").is(id)), Platform.class);
    }

    public Mono<Platform> save(Platform platform) {
        if (platform.getId() == null) {
            return masterTemplate.insert(platform);
        } else {
            return masterTemplate.update(platform);
        }
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate.delete(Platform.class).matching(Query.query(Criteria.where("id").is(id))).all().then();
    }
}

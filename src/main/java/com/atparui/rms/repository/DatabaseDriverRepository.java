package com.atparui.rms.repository;

import com.atparui.rms.domain.DriverJar;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DatabaseDriverRepository {

    private final R2dbcEntityTemplate masterTemplate;

    public DatabaseDriverRepository(@Qualifier("masterR2dbcTemplate") R2dbcEntityTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    public Flux<DriverJar> findAll() {
        return masterTemplate.select(DriverJar.class).all();
    }

    public Flux<DriverJar> findByVersionId(Long versionId) {
        return masterTemplate.select(DriverJar.class).matching(Query.query(Criteria.where("version_id").is(versionId))).all();
    }

    public Flux<DriverJar> findByVersionIdAndDriverType(Long versionId, String driverType) {
        return masterTemplate
            .select(DriverJar.class)
            .matching(Query.query(Criteria.where("version_id").is(versionId).and("driver_type").is(driverType).and("active").is(true)))
            .all();
    }

    public Mono<DriverJar> findById(Long id) {
        return masterTemplate.selectOne(Query.query(Criteria.where("id").is(id)), DriverJar.class);
    }

    public Mono<DriverJar> findDefaultDriver(Long versionId, String driverType) {
        return masterTemplate
            .select(DriverJar.class)
            .matching(
                Query.query(
                    Criteria.where("version_id")
                        .is(versionId)
                        .and("driver_type")
                        .is(driverType)
                        .and("is_default")
                        .is(true)
                        .and("active")
                        .is(true)
                )
            )
            .one();
    }

    public Mono<DriverJar> save(DriverJar driver) {
        if (driver.getId() == null) {
            return masterTemplate.insert(driver);
        } else {
            return masterTemplate.update(driver);
        }
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate.delete(DriverJar.class).matching(Query.query(Criteria.where("id").is(id))).all().then();
    }
}

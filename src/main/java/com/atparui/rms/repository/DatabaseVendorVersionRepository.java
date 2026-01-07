package com.atparui.rms.repository;

import com.atparui.rms.domain.DatabaseVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DatabaseVendorVersionRepository {

    private final R2dbcEntityTemplate masterTemplate;

    public DatabaseVendorVersionRepository(@Qualifier("masterR2dbcTemplate") R2dbcEntityTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    public Flux<DatabaseVersion> findAll() {
        return masterTemplate.select(DatabaseVersion.class).all();
    }

    public Flux<DatabaseVersion> findByDatabaseId(Long databaseId) {
        return masterTemplate.select(DatabaseVersion.class).matching(Query.query(Criteria.where("database_id").is(databaseId))).all();
    }

    public Flux<DatabaseVersion> findByDatabaseIdAndActiveTrue(Long databaseId) {
        return masterTemplate
            .select(DatabaseVersion.class)
            .matching(Query.query(Criteria.where("database_id").is(databaseId).and("active").is(true)))
            .all();
    }

    public Flux<DatabaseVersion> findRecentVersions(Long databaseId, int years) {
        // Get versions from last N years
        java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusYears(years);
        return masterTemplate
            .select(DatabaseVersion.class)
            .matching(
                Query.query(
                    Criteria.where("database_id").is(databaseId).and("active").is(true).and("release_date").greaterThanOrEquals(cutoffDate)
                )
            )
            .all();
    }

    public Mono<DatabaseVersion> findById(Long id) {
        return masterTemplate.selectOne(Query.query(Criteria.where("id").is(id)), DatabaseVersion.class);
    }

    public Mono<DatabaseVersion> findByDatabaseIdAndVersion(Long databaseId, String version) {
        return masterTemplate
            .select(DatabaseVersion.class)
            .matching(Query.query(Criteria.where("database_id").is(databaseId).and("version").is(version)))
            .one();
    }

    public Mono<DatabaseVersion> save(DatabaseVersion version) {
        if (version.getId() == null) {
            return masterTemplate.insert(version);
        } else {
            return masterTemplate.update(version);
        }
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate.delete(DatabaseVersion.class).matching(Query.query(Criteria.where("id").is(id))).all().then();
    }

    public Mono<Boolean> existsByDatabaseIdAndVersion(Long databaseId, String version) {
        return masterTemplate.exists(
            Query.query(Criteria.where("database_id").is(databaseId).and("version").is(version)),
            DatabaseVersion.class
        );
    }
}

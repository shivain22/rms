package com.atparui.rms.repository;

import com.atparui.rms.domain.DatabaseVendorVersion;
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

    public Flux<DatabaseVendorVersion> findAll() {
        return masterTemplate.select(DatabaseVendorVersion.class).all();
    }

    public Flux<DatabaseVendorVersion> findByDatabaseId(Long databaseId) {
        return masterTemplate.select(DatabaseVendorVersion.class).matching(Query.query(Criteria.where("database_id").is(databaseId))).all();
    }

    public Flux<DatabaseVendorVersion> findByDatabaseIdAndActiveTrue(Long databaseId) {
        return masterTemplate
            .select(DatabaseVendorVersion.class)
            .matching(Query.query(Criteria.where("database_id").is(databaseId).and("active").is(true)))
            .all();
    }

    public Flux<DatabaseVendorVersion> findRecentVersions(Long databaseId, int years) {
        // Get versions from last N years
        java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusYears(years);
        return masterTemplate
            .select(DatabaseVendorVersion.class)
            .matching(
                Query.query(
                    Criteria.where("database_id").is(databaseId).and("active").is(true).and("release_date").greaterThanOrEquals(cutoffDate)
                )
            )
            .all();
    }

    public Mono<DatabaseVendorVersion> findById(Long id) {
        return masterTemplate.selectOne(Query.query(Criteria.where("id").is(id)), DatabaseVendorVersion.class);
    }

    public Mono<DatabaseVendorVersion> findByDatabaseIdAndVersion(Long databaseId, String version) {
        return masterTemplate
            .select(DatabaseVendorVersion.class)
            .matching(Query.query(Criteria.where("database_id").is(databaseId).and("version").is(version)))
            .one();
    }

    public Mono<DatabaseVendorVersion> save(DatabaseVendorVersion version) {
        if (version.getId() == null) {
            return masterTemplate.insert(version);
        } else {
            return masterTemplate.update(version);
        }
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate.delete(DatabaseVendorVersion.class).matching(Query.query(Criteria.where("id").is(id))).all().then();
    }

    public Mono<Boolean> existsByDatabaseIdAndVersion(Long databaseId, String version) {
        return masterTemplate.exists(
            Query.query(Criteria.where("database_id").is(databaseId).and("version").is(version)),
            DatabaseVendorVersion.class
        );
    }
}

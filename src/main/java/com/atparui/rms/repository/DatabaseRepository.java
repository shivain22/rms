package com.atparui.rms.repository;

import com.atparui.rms.domain.Database;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DatabaseRepository {

    private final R2dbcEntityTemplate masterTemplate;

    public DatabaseRepository(@Qualifier("masterR2dbcTemplate") R2dbcEntityTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    public Flux<Database> findAll() {
        return masterTemplate.select(Database.class).all();
    }

    public Flux<Database> findAllActive() {
        return masterTemplate.select(Database.class).matching(Query.query(Criteria.where("active").is(true))).all();
    }

    public Flux<Database> findByVendorId(Long vendorId) {
        return masterTemplate.select(Database.class).matching(Query.query(Criteria.where("vendor_id").is(vendorId))).all();
    }

    public Flux<Database> findByVendorIdAndActiveTrue(Long vendorId) {
        return masterTemplate
            .select(Database.class)
            .matching(Query.query(Criteria.where("vendor_id").is(vendorId).and("active").is(true)))
            .all();
    }

    public Mono<Database> findById(Long id) {
        return masterTemplate.selectOne(Query.query(Criteria.where("id").is(id)), Database.class);
    }

    public Mono<Database> save(Database database) {
        if (database.getId() == null) {
            return masterTemplate.insert(database);
        } else {
            return masterTemplate.update(database);
        }
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate.delete(Database.class).matching(Query.query(Criteria.where("id").is(id))).all().then();
    }

    public Mono<Boolean> existsByVendorIdAndDatabaseCode(Long vendorId, String databaseCode) {
        return masterTemplate.exists(
            Query.query(Criteria.where("vendor_id").is(vendorId).and("database_code").is(databaseCode)),
            Database.class
        );
    }
}

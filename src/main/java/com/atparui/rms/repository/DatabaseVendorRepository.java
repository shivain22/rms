package com.atparui.rms.repository;

import com.atparui.rms.domain.DatabaseVendor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DatabaseVendorRepository {

    private final R2dbcEntityTemplate masterTemplate;

    public DatabaseVendorRepository(@Qualifier("masterR2dbcTemplate") R2dbcEntityTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    public Flux<DatabaseVendor> findAll() {
        return masterTemplate.select(DatabaseVendor.class).all();
    }

    public Flux<DatabaseVendor> findAllActive() {
        return masterTemplate.select(DatabaseVendor.class).matching(Query.query(Criteria.where("active").is(true))).all();
    }

    public Mono<DatabaseVendor> findById(Long id) {
        return masterTemplate.selectOne(Query.query(Criteria.where("id").is(id)), DatabaseVendor.class);
    }

    public Mono<DatabaseVendor> findByVendorCode(String vendorCode) {
        return masterTemplate.select(DatabaseVendor.class).matching(Query.query(Criteria.where("vendor_code").is(vendorCode))).one();
    }

    public Mono<DatabaseVendor> findByVendorCodeAndActiveTrue(String vendorCode) {
        return masterTemplate
            .select(DatabaseVendor.class)
            .matching(Query.query(Criteria.where("vendor_code").is(vendorCode).and("active").is(true)))
            .one();
    }

    public Mono<DatabaseVendor> save(DatabaseVendor databaseVendor) {
        if (databaseVendor.getId() == null) {
            return masterTemplate.insert(databaseVendor);
        } else {
            return masterTemplate.update(databaseVendor);
        }
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate.delete(DatabaseVendor.class).matching(Query.query(Criteria.where("id").is(id))).all().then();
    }

    public Mono<Boolean> existsByVendorCode(String vendorCode) {
        return masterTemplate.exists(Query.query(Criteria.where("vendor_code").is(vendorCode)), DatabaseVendor.class);
    }
}

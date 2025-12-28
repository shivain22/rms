package com.atparui.rms.repository;

import com.atparui.rms.domain.Tenant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class TenantRepository {

    private final R2dbcEntityTemplate masterTemplate;

    public TenantRepository(@Qualifier("masterR2dbcTemplate") R2dbcEntityTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    public Mono<Tenant> findByTenantIdAndActiveTrue(String tenantId) {
        return masterTemplate
            .select(Tenant.class)
            .matching(Query.query(Criteria.where("tenant_id").is(tenantId).and("active").is(true)))
            .one();
    }

    public Mono<Tenant> findByTenantKeyAndActiveTrue(String tenantKey) {
        return masterTemplate
            .select(Tenant.class)
            .matching(Query.query(Criteria.where("tenant_key").is(tenantKey).and("active").is(true)))
            .one();
    }

    public Mono<Tenant> findBySubdomainAndActiveTrue(String subdomain) {
        return masterTemplate
            .select(Tenant.class)
            .matching(Query.query(Criteria.where("subdomain").is(subdomain).and("active").is(true)))
            .one();
    }

    public Flux<Tenant> findAll() {
        return masterTemplate.select(Tenant.class).all();
    }

    public Mono<Tenant> findById(Long id) {
        return masterTemplate.selectOne(Query.query(Criteria.where("id").is(id)), Tenant.class);
    }

    public Mono<Tenant> save(Tenant tenant) {
        if (tenant.getId() == null) {
            return masterTemplate.insert(tenant);
        } else {
            return masterTemplate.update(tenant);
        }
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate
            .update(Tenant.class)
            .matching(Query.query(Criteria.where("id").is(id)))
            .apply(Update.update("active", false))
            .then();
    }

    public Mono<Boolean> existsByTenantId(String tenantId) {
        return masterTemplate.exists(Query.query(Criteria.where("tenant_id").is(tenantId)), Tenant.class);
    }

    public Mono<Boolean> existsByTenantKey(String tenantKey) {
        return masterTemplate.exists(Query.query(Criteria.where("tenant_key").is(tenantKey)), Tenant.class);
    }

    /**
     * Find tenant by tenantKey without checking active status.
     * This is useful for rollback operations where we need to find and delete tenants
     * regardless of their active status.
     *
     * @param tenantKey the tenant key
     * @return Mono containing the tenant if found
     */
    public Mono<Tenant> findByTenantKey(String tenantKey) {
        return masterTemplate.select(Tenant.class).matching(Query.query(Criteria.where("tenant_key").is(tenantKey))).one();
    }
}

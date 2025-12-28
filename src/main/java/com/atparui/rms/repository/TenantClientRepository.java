package com.atparui.rms.repository;

import com.atparui.rms.domain.TenantClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class TenantClientRepository {

    private final R2dbcEntityTemplate masterTemplate;

    public TenantClientRepository(@Qualifier("masterR2dbcTemplate") R2dbcEntityTemplate masterTemplate) {
        this.masterTemplate = masterTemplate;
    }

    public Flux<TenantClient> findByTenantId(Long tenantId) {
        return masterTemplate.select(TenantClient.class).matching(Query.query(Criteria.where("tenant_id").is(tenantId))).all();
    }

    public Flux<TenantClient> findByTenantIdAndEnabled(Long tenantId, Boolean enabled) {
        return masterTemplate
            .select(TenantClient.class)
            .matching(Query.query(Criteria.where("tenant_id").is(tenantId).and("enabled").is(enabled)))
            .all();
    }

    public Mono<TenantClient> findByTenantIdAndClientType(Long tenantId, String clientType) {
        return masterTemplate
            .select(TenantClient.class)
            .matching(Query.query(Criteria.where("tenant_id").is(tenantId).and("client_type").is(clientType)))
            .one();
    }

    public Mono<TenantClient> save(TenantClient tenantClient) {
        if (tenantClient.getId() == null) {
            return masterTemplate.insert(tenantClient);
        } else {
            return masterTemplate.update(tenantClient);
        }
    }

    public Flux<TenantClient> saveAll(Flux<TenantClient> tenantClients) {
        return tenantClients.flatMap(this::save);
    }

    public Mono<Void> deleteByTenantId(Long tenantId) {
        return masterTemplate.delete(TenantClient.class).matching(Query.query(Criteria.where("tenant_id").is(tenantId))).all().then();
    }

    public Mono<Void> deleteById(Long id) {
        return masterTemplate.delete(TenantClient.class).matching(Query.query(Criteria.where("id").is(id))).all().then();
    }
}

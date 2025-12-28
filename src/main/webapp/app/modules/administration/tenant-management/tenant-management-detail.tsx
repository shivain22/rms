import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Row, Col, Badge } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getTenant } from './tenant-management.reducer';

export const TenantManagementDetail = () => {
  const dispatch = useAppDispatch();
  const { id } = useParams<'id'>();

  const tenant = useAppSelector(state => state.tenantManagement.tenant);

  useEffect(() => {
    if (id) {
      dispatch(getTenant(id));
    }
  }, [id]);

  return (
    <div>
      <h2>
        <Translate contentKey="tenantManagement.detail.title">Tenant</Translate> [<strong>{tenant.tenantId}</strong>]
      </h2>
      <Row>
        <Col md="8">
          <dl className="jh-entity-details">
            <dt>
              <Translate contentKey="global.field.id">ID</Translate>
            </dt>
            <dd>{tenant.id}</dd>
            <dt>
              <Translate contentKey="tenantManagement.tenantKey">Tenant Key</Translate>
            </dt>
            <dd>{tenant.tenantKey}</dd>
            <dt>
              <Translate contentKey="tenantManagement.tenantId">Tenant ID</Translate>
            </dt>
            <dd>{tenant.tenantId}</dd>
            <dt>
              <Translate contentKey="tenantManagement.name">Name</Translate>
            </dt>
            <dd>{tenant.name}</dd>
            <dt>
              <Translate contentKey="tenantManagement.databaseUrl">Database URL</Translate>
            </dt>
            <dd>{tenant.databaseUrl}</dd>
            <dt>
              <Translate contentKey="tenantManagement.databaseUsername">Database Username</Translate>
            </dt>
            <dd>{tenant.databaseUsername}</dd>
            <dt>
              <Translate contentKey="tenantManagement.schemaName">Schema Name</Translate>
            </dt>
            <dd>{tenant.schemaName}</dd>
            <dt>
              <Translate contentKey="tenantManagement.active">Status</Translate>
            </dt>
            <dd>
              {tenant.active ? (
                <Badge color="success">
                  <Translate contentKey="tenantManagement.active">Active</Translate>
                </Badge>
              ) : (
                <Badge color="danger">
                  <Translate contentKey="tenantManagement.inactive">Inactive</Translate>
                </Badge>
              )}
            </dd>
          </dl>
          <Button tag={Link} to="/admin/tenant-management" replace color="info">
            <FontAwesomeIcon icon="arrow-left" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.back">Back</Translate>
            </span>
          </Button>
          &nbsp;
          <Button tag={Link} to={`/admin/tenant-management/${tenant.id}/edit`} replace color="primary">
            <FontAwesomeIcon icon="pencil-alt" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.edit">Edit</Translate>
            </span>
          </Button>
        </Col>
      </Row>
    </div>
  );
};

export default TenantManagementDetail;

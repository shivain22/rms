import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button, Table, Badge } from 'reactstrap';
import { Translate, getSortState } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSort, faSortUp, faSortDown } from '@fortawesome/free-solid-svg-icons';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getTenants } from './tenant-management.reducer';
import { ITenant } from './tenant.model';

const TenantManagement = () => {
  const dispatch = useAppDispatch();
  const [sort, setSort] = useState('id,asc');

  const tenantList = useAppSelector(state => state.tenantManagement.tenants);
  const loading = useAppSelector(state => state.tenantManagement.loading);

  useEffect(() => {
    dispatch(getTenants());
  }, []);

  const handleSort = (sortField: string) => {
    const order = sort.endsWith('asc') ? 'desc' : 'asc';
    setSort(`${sortField},${order}`);
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = sort.split(',')[0];
    const sortOrder = sort.split(',')[1];
    if (sortFieldName !== fieldName) {
      return faSort;
    } else {
      return sortOrder === 'asc' ? faSortUp : faSortDown;
    }
  };

  return (
    <div>
      <h2 id="tenant-management-page-heading" data-cy="tenantManagementPageHeading">
        <Translate contentKey="tenantManagement.home.title">Tenants</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={() => dispatch(getTenants())} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="tenantManagement.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/admin/tenant-management/new" className="btn btn-primary jh-create-entity">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="tenantManagement.home.createLabel">Create a new Tenant</Translate>
          </Link>
        </div>
      </h2>
      <Table responsive striped>
        <thead>
          <tr>
            <th className="hand" onClick={() => handleSort('id')}>
              <Translate contentKey="global.field.id">ID</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
            </th>
            <th className="hand" onClick={() => handleSort('tenantKey')}>
              <Translate contentKey="tenantManagement.tenantKey">Tenant Key</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('tenantKey')} />
            </th>
            <th className="hand" onClick={() => handleSort('tenantId')}>
              <Translate contentKey="tenantManagement.tenantId">Tenant ID</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('tenantId')} />
            </th>
            <th className="hand" onClick={() => handleSort('name')}>
              <Translate contentKey="tenantManagement.name">Name</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('name')} />
            </th>
            <th className="hand" onClick={() => handleSort('subdomain')}>
              <Translate contentKey="tenantManagement.subdomain">Subdomain</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('subdomain')} />
            </th>
            <th className="hand" onClick={() => handleSort('realmName')}>
              <Translate contentKey="tenantManagement.realmName">Realm</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('realmName')} />
            </th>
            <th className="hand" onClick={() => handleSort('active')}>
              <Translate contentKey="tenantManagement.active">Status</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('active')} />
            </th>
            <th />
          </tr>
        </thead>
        <tbody>
          {tenantList.map((tenant: ITenant, i) => (
            <tr id={tenant.id?.toString()} key={`tenant-${i}`}>
              <td>
                <Button tag={Link} to={`/admin/tenant-management/${tenant.id}`} color="link" size="sm">
                  {tenant.id}
                </Button>
              </td>
              <td>{tenant.tenantKey}</td>
              <td>{tenant.tenantId}</td>
              <td>{tenant.name}</td>
              <td>{tenant.subdomain}</td>
              <td>{tenant.realmName}</td>
              <td>
                {tenant.active ? (
                  <Badge color="success">
                    <Translate contentKey="tenantManagement.active">Active</Translate>
                  </Badge>
                ) : (
                  <Badge color="danger">
                    <Translate contentKey="tenantManagement.inactive">Inactive</Translate>
                  </Badge>
                )}
              </td>
              <td className="text-end">
                <div className="btn-group flex-btn-group-container">
                  <Button tag={Link} to={`/admin/tenant-management/${tenant.id}`} color="info" size="sm">
                    <FontAwesomeIcon icon="eye" />{' '}
                    <span className="d-none d-md-inline">
                      <Translate contentKey="entity.action.view">View</Translate>
                    </span>
                  </Button>
                  <Button tag={Link} to={`/admin/tenant-management/${tenant.id}/edit`} color="primary" size="sm">
                    <FontAwesomeIcon icon="pencil-alt" />{' '}
                    <span className="d-none d-md-inline">
                      <Translate contentKey="entity.action.edit">Edit</Translate>
                    </span>
                  </Button>
                  <Button tag={Link} to={`/admin/tenant-management/${tenant.id}/delete`} color="danger" size="sm" disabled={!tenant.active}>
                    <FontAwesomeIcon icon="trash" />{' '}
                    <span className="d-none d-md-inline">
                      <Translate contentKey="entity.action.delete">Delete</Translate>
                    </span>
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
};

export default TenantManagement;

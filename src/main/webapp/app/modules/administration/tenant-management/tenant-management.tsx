import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
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
          <Button className="me-2" variant="outline" onClick={() => dispatch(getTenants())} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="tenantManagement.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Button asChild variant="default">
            <Link to="/admin/tenant-management/new" className="jh-create-entity">
              <FontAwesomeIcon icon="plus" />
              &nbsp;
              <Translate contentKey="tenantManagement.home.createLabel">Create a new Tenant</Translate>
            </Link>
          </Button>
        </div>
      </h2>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="hand cursor-pointer" onClick={() => handleSort('id')}>
              <Translate contentKey="global.field.id">ID</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
            </TableHead>
            <TableHead className="hand cursor-pointer" onClick={() => handleSort('tenantKey')}>
              <Translate contentKey="tenantManagement.tenantKey">Tenant Key</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('tenantKey')} />
            </TableHead>
            <TableHead className="hand cursor-pointer" onClick={() => handleSort('tenantId')}>
              <Translate contentKey="tenantManagement.tenantId">Tenant ID</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('tenantId')} />
            </TableHead>
            <TableHead className="hand cursor-pointer" onClick={() => handleSort('name')}>
              <Translate contentKey="tenantManagement.name">Name</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('name')} />
            </TableHead>
            <TableHead className="hand cursor-pointer" onClick={() => handleSort('subdomain')}>
              <Translate contentKey="tenantManagement.subdomain">Subdomain</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('subdomain')} />
            </TableHead>
            <TableHead className="hand cursor-pointer" onClick={() => handleSort('realmName')}>
              <Translate contentKey="tenantManagement.realmName">Realm</Translate>{' '}
              <FontAwesomeIcon icon={getSortIconByFieldName('realmName')} />
            </TableHead>
            <TableHead className="hand cursor-pointer" onClick={() => handleSort('active')}>
              <Translate contentKey="tenantManagement.active">Status</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('active')} />
            </TableHead>
            <TableHead />
          </TableRow>
        </TableHeader>
        <TableBody>
          {tenantList.map((tenant: ITenant, i) => (
            <TableRow id={tenant.id?.toString()} key={`tenant-${i}`} className="even:bg-muted/50">
              <TableCell>
                <Button asChild variant="link" size="sm">
                  <Link to={`/admin/tenant-management/${tenant.id}`}>{tenant.id}</Link>
                </Button>
              </TableCell>
              <TableCell>{tenant.tenantKey}</TableCell>
              <TableCell>{tenant.tenantId}</TableCell>
              <TableCell>{tenant.name}</TableCell>
              <TableCell>{tenant.subdomain}</TableCell>
              <TableCell>{tenant.realmName}</TableCell>
              <TableCell>
                {tenant.active ? (
                  <Badge variant="default">
                    <Translate contentKey="tenantManagement.active">Active</Translate>
                  </Badge>
                ) : (
                  <Badge variant="destructive">
                    <Translate contentKey="tenantManagement.inactive">Inactive</Translate>
                  </Badge>
                )}
              </TableCell>
              <TableCell className="text-end">
                <div className="btn-group flex-btn-group-container">
                  <Button asChild variant="outline" size="sm">
                    <Link to={`/admin/tenant-management/${tenant.id}`}>
                      <FontAwesomeIcon icon="eye" />{' '}
                      <span className="d-none d-md-inline">
                        <Translate contentKey="entity.action.view">View</Translate>
                      </span>
                    </Link>
                  </Button>
                  <Button asChild variant="default" size="sm">
                    <Link to={`/admin/tenant-management/${tenant.id}/edit`}>
                      <FontAwesomeIcon icon="pencil-alt" />{' '}
                      <span className="d-none d-md-inline">
                        <Translate contentKey="entity.action.edit">Edit</Translate>
                      </span>
                    </Link>
                  </Button>
                  <Button asChild variant="destructive" size="sm" disabled={!tenant.active}>
                    <Link to={`/admin/tenant-management/${tenant.id}/delete`}>
                      <FontAwesomeIcon icon="trash" />{' '}
                      <span className="d-none d-md-inline">
                        <Translate contentKey="entity.action.delete">Delete</Translate>
                      </span>
                    </Link>
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};

export default TenantManagement;

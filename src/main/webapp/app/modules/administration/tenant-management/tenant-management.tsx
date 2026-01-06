import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Translate } from 'react-jhipster';
import { RefreshCw, Plus, ArrowUpDown, ArrowUp, ArrowDown, Eye, Pencil, Trash2, MoreHorizontal } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

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
      return <ArrowUpDown className="h-3 w-3 ml-1" />;
    } else {
      return sortOrder === 'asc' ? <ArrowUp className="h-3 w-3 ml-1" /> : <ArrowDown className="h-3 w-3 ml-1" />;
    }
  };

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="tenant-management-page-heading" data-cy="tenantManagementPageHeading">
            <Translate contentKey="tenantManagement.home.title">Tenants</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="tenantManagement.home.subtitle">Manage and configure tenant settings</Translate>
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => dispatch(getTenants())} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            <Translate contentKey="tenantManagement.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Button asChild variant="default">
            <Link to="/admin/tenant-management/new" className="jh-create-entity">
              <Plus className="mr-2 h-4 w-4" />
              <Translate contentKey="tenantManagement.home.createLabel">Create a new Tenant</Translate>
            </Link>
          </Button>
        </div>
      </div>

      {/* Tenants Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <Translate contentKey="tenantManagement.table.title">Tenant List</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="tenantManagement.table.description">View and manage all registered tenants</Translate>
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow className="border-b">
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('id')}>
                    <div className="flex items-center">
                      <Translate contentKey="global.field.id">ID</Translate>
                      {getSortIconByFieldName('id')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('tenantKey')}>
                    <div className="flex items-center">
                      <Translate contentKey="tenantManagement.tenantKey">Tenant Key</Translate>
                      {getSortIconByFieldName('tenantKey')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('tenantId')}>
                    <div className="flex items-center">
                      <Translate contentKey="tenantManagement.tenantId">Tenant ID</Translate>
                      {getSortIconByFieldName('tenantId')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('name')}>
                    <div className="flex items-center">
                      <Translate contentKey="tenantManagement.name">Name</Translate>
                      {getSortIconByFieldName('name')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('subdomain')}>
                    <div className="flex items-center">
                      <Translate contentKey="tenantManagement.subdomain">Subdomain</Translate>
                      {getSortIconByFieldName('subdomain')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('realmName')}>
                    <div className="flex items-center">
                      <Translate contentKey="tenantManagement.realmName">Realm</Translate>
                      {getSortIconByFieldName('realmName')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('active')}>
                    <div className="flex items-center">
                      <Translate contentKey="tenantManagement.active">Status</Translate>
                      {getSortIconByFieldName('active')}
                    </div>
                  </TableHead>
                  <TableHead className="w-[100px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tenantList && tenantList.length > 0 ? (
                  tenantList.map((tenant: ITenant, i) => (
                    <TableRow id={tenant.id?.toString()} key={`tenant-${i}`} className="border-b hover:bg-muted/50">
                      <TableCell className="font-medium">
                        <Button asChild variant="link" size="sm" className="h-auto p-0 font-medium">
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
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                              <span className="sr-only">Open menu</span>
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/tenant-management/${tenant.id}`} className="flex items-center">
                                <Eye className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.view">View</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/tenant-management/${tenant.id}/edit`} className="flex items-center">
                                <Pencil className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.edit">Edit</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild disabled={!tenant.active}>
                              <Link
                                to={`/admin/tenant-management/${tenant.id}/delete`}
                                className="flex items-center text-destructive focus:text-destructive"
                              >
                                <Trash2 className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.delete">Delete</Translate>
                              </Link>
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={8} className="h-24 text-center text-muted-foreground">
                      <Translate contentKey="tenantManagement.table.empty">No tenants found</Translate>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default TenantManagement;

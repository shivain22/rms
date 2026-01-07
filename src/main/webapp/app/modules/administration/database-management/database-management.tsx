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
import { getDatabases, deleteDatabase } from './database.reducer';
import { IDatabase } from './database.model';
import { getDatabaseVendors } from './database-vendor.reducer';

const DatabaseManagement = () => {
  const dispatch = useAppDispatch();
  const [sort, setSort] = useState('id,asc');

  const databaseList = useAppSelector(state => state.databaseManagement.databases);
  const loading = useAppSelector(state => state.databaseManagement.loading);
  const vendors = useAppSelector(state => state.databaseVendorManagement.databaseVendors);

  useEffect(() => {
    dispatch(getDatabases());
    dispatch(getDatabaseVendors());
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

  const getVendorName = (vendorId: number) => {
    const vendor = vendors.find(v => v.id === vendorId);
    return vendor?.displayName || `Vendor ${vendorId}`;
  };

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="database-management-page-heading">
            <Translate contentKey="databaseManagement.home.title">Databases</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="databaseManagement.home.subtitle">Manage database products from vendors</Translate>
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => dispatch(getDatabases())} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            <Translate contentKey="databaseManagement.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Button asChild variant="default">
            <Link to="/admin/database-management/new" className="jh-create-entity">
              <Plus className="mr-2 h-4 w-4" />
              <Translate contentKey="databaseManagement.home.createLabel">Create a new Database</Translate>
            </Link>
          </Button>
        </div>
      </div>

      {/* Databases Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <Translate contentKey="databaseManagement.table.title">Database List</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="databaseManagement.table.description">View and manage all databases</Translate>
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
                  <TableHead>
                    <Translate contentKey="databaseManagement.vendor">Vendor</Translate>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('databaseCode')}>
                    <div className="flex items-center">
                      <Translate contentKey="databaseManagement.databaseCode">Database Code</Translate>
                      {getSortIconByFieldName('databaseCode')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('displayName')}>
                    <div className="flex items-center">
                      <Translate contentKey="databaseManagement.displayName">Display Name</Translate>
                      {getSortIconByFieldName('displayName')}
                    </div>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="databaseManagement.defaultPort">Default Port</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="databaseManagement.active">Active</Translate>
                  </TableHead>
                  <TableHead className="text-right">
                    <Translate contentKey="global.field.actions">Actions</Translate>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8">
                      <RefreshCw className="h-6 w-6 animate-spin mx-auto" />
                    </TableCell>
                  </TableRow>
                ) : databaseList && databaseList.length > 0 ? (
                  databaseList.map((database: IDatabase) => (
                    <TableRow key={database.id} className="border-b">
                      <TableCell>{database.id}</TableCell>
                      <TableCell>{getVendorName(database.vendorId)}</TableCell>
                      <TableCell className="font-medium">{database.databaseCode}</TableCell>
                      <TableCell>{database.displayName}</TableCell>
                      <TableCell>{database.defaultPort}</TableCell>
                      <TableCell>
                        <Badge variant={database.active ? 'default' : 'secondary'}>{database.active ? 'Active' : 'Inactive'}</Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="sm">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/database-management/${database.id}`}>
                                <Eye className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.view">View</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/database-management/${database.id}/edit`}>
                                <Pencil className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.edit">Edit</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/database-management/${database.id}/delete`}>
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
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      <Translate contentKey="databaseManagement.table.empty">No databases found</Translate>
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

export default DatabaseManagement;

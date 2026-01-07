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
import { getDatabaseVersions, deleteDatabaseVersion } from './database-version.reducer';
import { IDatabaseVersion } from './database-version.model';
import { getDatabases } from './database.reducer';

const DatabaseVersionManagement = () => {
  const dispatch = useAppDispatch();
  const [sort, setSort] = useState('id,asc');

  const versionList = useAppSelector(state => state.databaseVersionManagement.databaseVersions);
  const loading = useAppSelector(state => state.databaseVersionManagement.loading);
  const databases = useAppSelector(state => state.databaseManagement.databases);

  useEffect(() => {
    dispatch(getDatabaseVersions());
    dispatch(getDatabases());
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

  const getDatabaseName = (databaseId: number) => {
    const database = databases.find(d => d.id === databaseId);
    return database?.displayName || `Database ${databaseId}`;
  };

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="database-version-management-page-heading">
            <Translate contentKey="databaseVersionManagement.home.title">Database Versions</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="databaseVersionManagement.home.subtitle">Manage database versions</Translate>
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => dispatch(getDatabaseVersions())} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            <Translate contentKey="databaseVersionManagement.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Button asChild variant="default">
            <Link to="/admin/database-version-management/new" className="jh-create-entity">
              <Plus className="mr-2 h-4 w-4" />
              <Translate contentKey="databaseVersionManagement.home.createLabel">Create a new Database Version</Translate>
            </Link>
          </Button>
        </div>
      </div>

      {/* Versions Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <Translate contentKey="databaseVersionManagement.table.title">Database Version List</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="databaseVersionManagement.table.description">View and manage all database versions</Translate>
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
                    <Translate contentKey="databaseVersionManagement.database">Database</Translate>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('version')}>
                    <div className="flex items-center">
                      <Translate contentKey="databaseVersionManagement.version">Version</Translate>
                      {getSortIconByFieldName('version')}
                    </div>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="databaseVersionManagement.displayName">Display Name</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="databaseVersionManagement.isSupported">Supported</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="databaseVersionManagement.isRecommended">Recommended</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="databaseVersionManagement.active">Active</Translate>
                  </TableHead>
                  <TableHead className="text-right">
                    <Translate contentKey="global.field.actions">Actions</Translate>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8">
                      <RefreshCw className="h-6 w-6 animate-spin mx-auto" />
                    </TableCell>
                  </TableRow>
                ) : versionList && versionList.length > 0 ? (
                  versionList.map((version: IDatabaseVersion) => (
                    <TableRow key={version.id} className="border-b">
                      <TableCell>{version.id}</TableCell>
                      <TableCell>{getDatabaseName(version.databaseId)}</TableCell>
                      <TableCell className="font-medium">{version.version}</TableCell>
                      <TableCell>{version.displayName}</TableCell>
                      <TableCell>
                        <Badge variant={version.isSupported ? 'default' : 'secondary'}>{version.isSupported ? 'Yes' : 'No'}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={version.isRecommended ? 'default' : 'outline'}>{version.isRecommended ? 'Yes' : 'No'}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={version.active ? 'default' : 'secondary'}>{version.active ? 'Active' : 'Inactive'}</Badge>
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
                              <Link to={`/admin/database-version-management/${version.id}`}>
                                <Eye className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.view">View</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/database-version-management/${version.id}/edit`}>
                                <Pencil className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.edit">Edit</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/database-version-management/${version.id}/delete`}>
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
                    <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                      <Translate contentKey="databaseVersionManagement.table.empty">No database versions found</Translate>
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

export default DatabaseVersionManagement;

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
import { getPlatforms } from './platform.reducer';
import { IPlatform } from './platform.model';

const PlatformManagement = () => {
  const dispatch = useAppDispatch();
  const [sort, setSort] = useState('id,asc');

  const platformList = useAppSelector(state => state.platform.platforms);
  const loading = useAppSelector(state => state.platform.loading);

  useEffect(() => {
    dispatch(getPlatforms());
  }, [dispatch]);

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

  // Sort platforms based on sort state
  const sortedPlatforms = [...platformList].sort((a, b) => {
    const [field, order] = sort.split(',');
    const aValue = a[field as keyof IPlatform];
    const bValue = b[field as keyof IPlatform];

    if (aValue === undefined || aValue === null) return 1;
    if (bValue === undefined || bValue === null) return -1;

    if (typeof aValue === 'string' && typeof bValue === 'string') {
      return order === 'asc' ? aValue.localeCompare(bValue) : bValue.localeCompare(aValue);
    }

    if (typeof aValue === 'number' && typeof bValue === 'number') {
      return order === 'asc' ? aValue - bValue : bValue - aValue;
    }

    return 0;
  });

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="platform-management-page-heading" data-cy="platformManagementPageHeading">
            <Translate contentKey="platformManagement.home.title">Platforms</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="platformManagement.home.subtitle">Manage platform types</Translate>
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => dispatch(getPlatforms())} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            <Translate contentKey="platformManagement.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Button asChild variant="default">
            <Link to="/admin/platform-management/new" className="jh-create-entity">
              <Plus className="mr-2 h-4 w-4" />
              <Translate contentKey="platformManagement.home.createLabel">Create a new Platform</Translate>
            </Link>
          </Button>
        </div>
      </div>

      {/* Platforms Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <Translate contentKey="platformManagement.table.title">Platform List</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="platformManagement.table.description">View and manage all platform types</Translate>
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
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('name')}>
                    <div className="flex items-center">
                      <Translate contentKey="platformManagement.name">Name</Translate>
                      {getSortIconByFieldName('name')}
                    </div>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="platformManagement.description">Description</Translate>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('active')}>
                    <div className="flex items-center">
                      <Translate contentKey="platformManagement.active">Status</Translate>
                      {getSortIconByFieldName('active')}
                    </div>
                  </TableHead>
                  <TableHead className="w-[100px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sortedPlatforms && sortedPlatforms.length > 0 ? (
                  sortedPlatforms.map((platform: IPlatform, i) => (
                    <TableRow id={platform.id?.toString()} key={`platform-${i}`} className="border-b hover:bg-muted/50">
                      <TableCell className="font-medium">
                        <Button asChild variant="link" size="sm" className="h-auto p-0 font-medium">
                          <Link to={`/admin/platform-management/${platform.id}`}>{platform.id}</Link>
                        </Button>
                      </TableCell>
                      <TableCell>{platform.name}</TableCell>
                      <TableCell className="max-w-md truncate">{platform.description}</TableCell>
                      <TableCell>
                        {platform.active !== false ? (
                          <Badge variant="default">
                            <Translate contentKey="platformManagement.active">Active</Translate>
                          </Badge>
                        ) : (
                          <Badge variant="destructive">
                            <Translate contentKey="platformManagement.inactive">Inactive</Translate>
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
                              <Link to={`/admin/platform-management/${platform.id}`} className="flex items-center">
                                <Eye className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.view">View</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/platform-management/${platform.id}/edit`} className="flex items-center">
                                <Pencil className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.edit">Edit</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link
                                to={`/admin/platform-management/${platform.id}/delete`}
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
                    <TableCell colSpan={5} className="h-24 text-center text-muted-foreground">
                      <Translate contentKey="platformManagement.table.empty">No platforms found</Translate>
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

export default PlatformManagement;

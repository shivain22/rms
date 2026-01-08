import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { RefreshCw, Plus, ArrowUpDown, ArrowUp, ArrowDown, Eye, Pencil, Trash2, MoreHorizontal } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDatabaseVendors, deleteDatabaseVendor } from './database-vendor.reducer';
import { IDatabaseVendor } from './database-vendor.model';

const DatabaseVendorManagement = () => {
  const dispatch = useAppDispatch();
  const [sort, setSort] = useState('id,asc');

  const vendorList = useAppSelector(state => state.databaseVendorManagement.databaseVendors);
  const loading = useAppSelector(state => state.databaseVendorManagement.loading);

  useEffect(() => {
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

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this database vendor?')) {
      dispatch(deleteDatabaseVendor(id));
    }
  };

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="database-vendor-management-page-heading">
            Database Vendors
          </h1>
          <p className="text-muted-foreground mt-1.5">Manage database vendors and providers</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => dispatch(getDatabaseVendors())} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh List
          </Button>
          <Button asChild variant="default">
            <Link to="/admin/database-vendor-management/new" className="jh-create-entity">
              <Plus className="mr-2 h-4 w-4" />
              Create a new Database Vendor
            </Link>
          </Button>
        </div>
      </div>

      {/* Vendors Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Database Vendor List</CardTitle>
            <CardDescription>View and manage all database vendors</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow className="border-b">
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('id')}>
                    <div className="flex items-center">
                      ID
                      {getSortIconByFieldName('id')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('vendorCode')}>
                    <div className="flex items-center">
                      Vendor Code
                      {getSortIconByFieldName('vendorCode')}
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('displayName')}>
                    <div className="flex items-center">
                      Display Name
                      {getSortIconByFieldName('displayName')}
                    </div>
                  </TableHead>
                  <TableHead>Default Port</TableHead>
                  <TableHead>Driver Key</TableHead>
                  <TableHead>Active</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8">
                      <RefreshCw className="h-6 w-6 animate-spin mx-auto" />
                    </TableCell>
                  </TableRow>
                ) : vendorList && vendorList.length > 0 ? (
                  vendorList.map((vendor: IDatabaseVendor) => (
                    <TableRow key={vendor.id} className="border-b">
                      <TableCell>{vendor.id}</TableCell>
                      <TableCell className="font-medium">{vendor.vendorCode}</TableCell>
                      <TableCell>{vendor.displayName}</TableCell>
                      <TableCell>{vendor.defaultPort}</TableCell>
                      <TableCell>{vendor.driverKey}</TableCell>
                      <TableCell>
                        <Badge variant={vendor.active ? 'default' : 'secondary'}>{vendor.active ? 'Active' : 'Inactive'}</Badge>
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
                              <Link to={`/admin/database-vendor-management/${vendor.id}`}>
                                <Eye className="mr-2 h-4 w-4" />
                                View
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/database-vendor-management/${vendor.id}/edit`}>
                                <Pencil className="mr-2 h-4 w-4" />
                                Edit
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/database-vendor-management/${vendor.id}/delete`}>
                                <Trash2 className="mr-2 h-4 w-4" />
                                Delete
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
                      No database vendors found
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

export default DatabaseVendorManagement;

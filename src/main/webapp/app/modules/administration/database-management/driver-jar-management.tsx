import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Translate } from 'react-jhipster';
import { RefreshCw, Plus, ArrowUpDown, ArrowUp, ArrowDown, Eye, Pencil, Trash2, MoreHorizontal, Upload } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDriverJars, deleteDriverJar } from './driver-jar.reducer';
import { IDriverJar } from './driver-jar.model';
import { getDatabaseVersions } from './database-version.reducer';

const DriverJarManagement = () => {
  const dispatch = useAppDispatch();
  const [sort, setSort] = useState('id,asc');

  const driverList = useAppSelector(state => state.driverJarManagement.driverJars);
  const loading = useAppSelector(state => state.driverJarManagement.loading);
  const versions = useAppSelector(state => state.databaseVersionManagement.databaseVersions);

  useEffect(() => {
    dispatch(getDriverJars());
    dispatch(getDatabaseVersions());
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

  const getVersionName = (versionId: number) => {
    const version = versions.find(v => v.id === versionId);
    return version ? `${version.displayName || version.version}` : `Version ${versionId}`;
  };

  const formatFileSize = (bytes: number) => {
    if (!bytes) return 'N/A';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="driver-jar-management-page-heading">
            <Translate contentKey="driverJarManagement.home.title">Driver Jars</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="driverJarManagement.home.subtitle">Manage JDBC/R2DBC driver JAR files</Translate>
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => dispatch(getDriverJars())} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            <Translate contentKey="driverJarManagement.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Button asChild variant="default">
            <Link to="/admin/driver-jar-management/new" className="jh-create-entity">
              <Upload className="mr-2 h-4 w-4" />
              <Translate contentKey="driverJarManagement.home.uploadLabel">Upload Driver JAR</Translate>
            </Link>
          </Button>
        </div>
      </div>

      {/* Driver Jars Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <Translate contentKey="driverJarManagement.table.title">Driver JAR List</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="driverJarManagement.table.description">View and manage all uploaded driver JAR files</Translate>
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
                    <Translate contentKey="driverJarManagement.version">Version</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="driverJarManagement.driverType">Driver Type</Translate>
                  </TableHead>
                  <TableHead className="cursor-pointer hover:bg-muted/50" onClick={() => handleSort('fileName')}>
                    <div className="flex items-center">
                      <Translate contentKey="driverJarManagement.fileName">File Name</Translate>
                      {getSortIconByFieldName('fileName')}
                    </div>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="driverJarManagement.fileSize">File Size</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="driverJarManagement.driverClassName">Driver Class</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="driverJarManagement.isDefault">Default</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="driverJarManagement.active">Active</Translate>
                  </TableHead>
                  <TableHead className="text-right">
                    <Translate contentKey="global.field.actions">Actions</Translate>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={9} className="text-center py-8">
                      <RefreshCw className="h-6 w-6 animate-spin mx-auto" />
                    </TableCell>
                  </TableRow>
                ) : driverList && driverList.length > 0 ? (
                  driverList.map((driver: IDriverJar) => (
                    <TableRow key={driver.id} className="border-b">
                      <TableCell>{driver.id}</TableCell>
                      <TableCell>{getVersionName(driver.versionId)}</TableCell>
                      <TableCell>
                        <Badge variant={driver.driverType === 'JDBC' ? 'default' : 'secondary'}>{driver.driverType}</Badge>
                      </TableCell>
                      <TableCell className="font-medium">{driver.fileName}</TableCell>
                      <TableCell>{formatFileSize(driver.fileSize)}</TableCell>
                      <TableCell className="font-mono text-sm">{driver.driverClassName}</TableCell>
                      <TableCell>
                        <Badge variant={driver.isDefault ? 'default' : 'outline'}>{driver.isDefault ? 'Yes' : 'No'}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={driver.active ? 'default' : 'secondary'}>{driver.active ? 'Active' : 'Inactive'}</Badge>
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
                              <Link to={`/admin/driver-jar-management/${driver.id}`}>
                                <Eye className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.view">View</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/driver-jar-management/${driver.id}/edit`}>
                                <Pencil className="mr-2 h-4 w-4" />
                                <Translate contentKey="entity.action.edit">Edit</Translate>
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link to={`/admin/driver-jar-management/${driver.id}/delete`}>
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
                    <TableCell colSpan={9} className="text-center py-8 text-muted-foreground">
                      <Translate contentKey="driverJarManagement.table.empty">No driver JARs found</Translate>
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

export default DriverJarManagement;

import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Pencil } from 'lucide-react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDriverJar } from './driver-jar.reducer';
import { getDatabaseVersions } from './database-version.reducer';

export const DriverJarManagementDetail = () => {
  const dispatch = useAppDispatch();
  const { id } = useParams<'id'>();

  const driver = useAppSelector(state => state.driverJarManagement.driverJar);
  const loading = useAppSelector(state => state.driverJarManagement.loading);
  const versions = useAppSelector(state => state.databaseVersionManagement.databaseVersions);

  useEffect(() => {
    if (id) {
      dispatch(getDriverJar(id));
    }
    dispatch(getDatabaseVersions());
  }, [id]);

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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Driver JAR
            {driver && <span className="ml-2 text-muted-foreground">[{driver.fileName}]</span>}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/driver-jar-management">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back
            </Link>
          </Button>
          {driver && (
            <Button asChild variant="default">
              <Link to={`/admin/driver-jar-management/${driver.id}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                Edit
              </Link>
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">Loading...</div>
      ) : driver ? (
        <Card>
          <CardHeader>
            <CardTitle>Driver JAR Information</CardTitle>
            <CardDescription>View driver JAR details</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">ID</label>
                <p className="text-base">{driver.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Version</label>
                <p className="text-base">{getVersionName(driver.versionId)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Driver Type</label>
                <p className="text-base">
                  <Badge variant={driver.driverType === 'JDBC' ? 'default' : 'secondary'}>{driver.driverType}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">File Name</label>
                <p className="text-base font-medium">{driver.fileName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">File Size</label>
                <p className="text-base">{formatFileSize(driver.fileSize)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Default</label>
                <p className="text-base">
                  <Badge variant={driver.isDefault ? 'default' : 'outline'}>{driver.isDefault ? 'Yes' : 'No'}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Status</label>
                <p className="text-base">
                  <Badge variant={driver.active ? 'default' : 'secondary'}>{driver.active ? 'Active' : 'Inactive'}</Badge>
                </p>
              </div>
              {driver.driverClassName && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">Driver Class Name</label>
                  <p className="text-base font-mono text-sm">{driver.driverClassName}</p>
                </div>
              )}
              {driver.filePath && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">File Path</label>
                  <p className="text-base font-mono text-sm">{driver.filePath}</p>
                </div>
              )}
              {driver.md5Hash && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">MD5 Hash</label>
                  <p className="text-base font-mono text-sm">{driver.md5Hash}</p>
                </div>
              )}
              {driver.description && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">Description</label>
                  <p className="text-base">{driver.description}</p>
                </div>
              )}
              {driver.uploadedBy && (
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Uploaded By</label>
                  <p className="text-base">{driver.uploadedBy}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">Driver JAR not found</CardContent>
        </Card>
      )}
    </div>
  );
};

export default DriverJarManagementDetail;

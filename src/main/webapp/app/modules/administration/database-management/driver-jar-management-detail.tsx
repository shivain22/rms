import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Translate } from 'react-jhipster';
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
            <Translate contentKey="driverJarManagement.detail.title">Driver JAR</Translate>
            {driver && <span className="ml-2 text-muted-foreground">[{driver.fileName}]</span>}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/driver-jar-management">
              <ArrowLeft className="mr-2 h-4 w-4" />
              <Translate contentKey="entity.action.back">Back</Translate>
            </Link>
          </Button>
          {driver && (
            <Button asChild variant="default">
              <Link to={`/admin/driver-jar-management/${driver.id}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                <Translate contentKey="entity.action.edit">Edit</Translate>
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
            <CardTitle>
              <Translate contentKey="driverJarManagement.detail.information">Driver JAR Information</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="driverJarManagement.detail.description">View driver JAR details</Translate>
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="global.field.id">ID</Translate>
                </label>
                <p className="text-base">{driver.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="driverJarManagement.version">Version</Translate>
                </label>
                <p className="text-base">{getVersionName(driver.versionId)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="driverJarManagement.driverType">Driver Type</Translate>
                </label>
                <p className="text-base">
                  <Badge variant={driver.driverType === 'JDBC' ? 'default' : 'secondary'}>{driver.driverType}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="driverJarManagement.fileName">File Name</Translate>
                </label>
                <p className="text-base font-medium">{driver.fileName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="driverJarManagement.fileSize">File Size</Translate>
                </label>
                <p className="text-base">{formatFileSize(driver.fileSize)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="driverJarManagement.isDefault">Default</Translate>
                </label>
                <p className="text-base">
                  <Badge variant={driver.isDefault ? 'default' : 'outline'}>{driver.isDefault ? 'Yes' : 'No'}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="driverJarManagement.active">Status</Translate>
                </label>
                <p className="text-base">
                  <Badge variant={driver.active ? 'default' : 'secondary'}>{driver.active ? 'Active' : 'Inactive'}</Badge>
                </p>
              </div>
              {driver.driverClassName && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="driverJarManagement.driverClassName">Driver Class Name</Translate>
                  </label>
                  <p className="text-base font-mono text-sm">{driver.driverClassName}</p>
                </div>
              )}
              {driver.filePath && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="driverJarManagement.filePath">File Path</Translate>
                  </label>
                  <p className="text-base font-mono text-sm">{driver.filePath}</p>
                </div>
              )}
              {driver.md5Hash && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="driverJarManagement.md5Hash">MD5 Hash</Translate>
                  </label>
                  <p className="text-base font-mono text-sm">{driver.md5Hash}</p>
                </div>
              )}
              {driver.description && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="driverJarManagement.description">Description</Translate>
                  </label>
                  <p className="text-base">{driver.description}</p>
                </div>
              )}
              {driver.uploadedBy && (
                <div>
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="driverJarManagement.uploadedBy">Uploaded By</Translate>
                  </label>
                  <p className="text-base">{driver.uploadedBy}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            <Translate contentKey="driverJarManagement.detail.notFound">Driver JAR not found</Translate>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default DriverJarManagementDetail;

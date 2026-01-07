import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Translate } from 'react-jhipster';
import { ArrowLeft, Pencil } from 'lucide-react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDatabaseVersion } from './database-version.reducer';
import { getDatabases } from './database.reducer';

export const DatabaseVersionManagementDetail = () => {
  const dispatch = useAppDispatch();
  const { id } = useParams<'id'>();

  const version = useAppSelector(state => state.databaseVersionManagement.databaseVersion);
  const loading = useAppSelector(state => state.databaseVersionManagement.loading);
  const databases = useAppSelector(state => state.databaseManagement.databases);

  useEffect(() => {
    if (id) {
      dispatch(getDatabaseVersion(id));
    }
    dispatch(getDatabases());
  }, [id]);

  const getDatabaseName = (databaseId: number) => {
    const database = databases.find(d => d.id === databaseId);
    return database?.displayName || `Database ${databaseId}`;
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey="databaseVersionManagement.detail.title">Database Version</Translate>
            {version && <span className="ml-2 text-muted-foreground">[{version.version}]</span>}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/database-version-management">
              <ArrowLeft className="mr-2 h-4 w-4" />
              <Translate contentKey="entity.action.back">Back</Translate>
            </Link>
          </Button>
          {version && (
            <Button asChild variant="default">
              <Link to={`/admin/database-version-management/${version.id}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                <Translate contentKey="entity.action.edit">Edit</Translate>
              </Link>
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">Loading...</div>
      ) : version ? (
        <Card>
          <CardHeader>
            <CardTitle>
              <Translate contentKey="databaseVersionManagement.detail.information">Version Information</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="databaseVersionManagement.detail.description">View database version details</Translate>
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="global.field.id">ID</Translate>
                </label>
                <p className="text-base">{version.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.database">Database</Translate>
                </label>
                <p className="text-base">{getDatabaseName(version.databaseId)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.version">Version</Translate>
                </label>
                <p className="text-base font-medium">{version.version}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.displayName">Display Name</Translate>
                </label>
                <p className="text-base">{version.displayName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.releaseDate">Release Date</Translate>
                </label>
                <p className="text-base">{version.releaseDate || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.endOfLifeDate">End of Life Date</Translate>
                </label>
                <p className="text-base">{version.endOfLifeDate || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.isSupported">Supported</Translate>
                </label>
                <p className="text-base">
                  <Badge variant={version.isSupported ? 'default' : 'secondary'}>{version.isSupported ? 'Yes' : 'No'}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.isRecommended">Recommended</Translate>
                </label>
                <p className="text-base">
                  <Badge variant={version.isRecommended ? 'default' : 'outline'}>{version.isRecommended ? 'Yes' : 'No'}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVersionManagement.active">Status</Translate>
                </label>
                <p className="text-base">
                  <Badge variant={version.active ? 'default' : 'secondary'}>{version.active ? 'Active' : 'Inactive'}</Badge>
                </p>
              </div>
              {version.releaseNotes && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="databaseVersionManagement.releaseNotes">Release Notes</Translate>
                  </label>
                  <p className="text-base">{version.releaseNotes}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            <Translate contentKey="databaseVersionManagement.detail.notFound">Database version not found</Translate>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default DatabaseVersionManagementDetail;

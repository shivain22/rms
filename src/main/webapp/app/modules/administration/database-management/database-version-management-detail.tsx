import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
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
            Database Version
            {version && <span className="ml-2 text-muted-foreground">[{version.version}]</span>}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/database-version-management">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back
            </Link>
          </Button>
          {version && (
            <Button asChild variant="default">
              <Link to={`/admin/database-version-management/${version.id}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                Edit
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
            <CardTitle>Version Information</CardTitle>
            <CardDescription>View database version details</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">ID</label>
                <p className="text-base">{version.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Database</label>
                <p className="text-base">{getDatabaseName(version.databaseId)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Version</label>
                <p className="text-base font-medium">{version.version}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Display Name</label>
                <p className="text-base">{version.displayName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Release Date</label>
                <p className="text-base">{version.releaseDate || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">End of Life Date</label>
                <p className="text-base">{version.endOfLifeDate || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Supported</label>
                <p className="text-base">
                  <Badge variant={version.isSupported ? 'default' : 'secondary'}>{version.isSupported ? 'Yes' : 'No'}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Recommended</label>
                <p className="text-base">
                  <Badge variant={version.isRecommended ? 'default' : 'outline'}>{version.isRecommended ? 'Yes' : 'No'}</Badge>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Status</label>
                <p className="text-base">
                  <Badge variant={version.active ? 'default' : 'secondary'}>{version.active ? 'Active' : 'Inactive'}</Badge>
                </p>
              </div>
              {version.releaseNotes && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">Release Notes</label>
                  <p className="text-base">{version.releaseNotes}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">Database version not found</CardContent>
        </Card>
      )}
    </div>
  );
};

export default DatabaseVersionManagementDetail;

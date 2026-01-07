import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Translate } from 'react-jhipster';
import { ArrowLeft, Pencil, Loader2 } from 'lucide-react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getPlatform } from './platform.reducer';

export const PlatformManagementDetail = () => {
  const dispatch = useAppDispatch();
  const { id } = useParams<'id'>();

  const platform = useAppSelector(state => state.platform.platforms.find(p => p.id?.toString() === id));
  const loading = useAppSelector(state => state.platform.loading);

  useEffect(() => {
    if (id) {
      dispatch(getPlatform(id));
    }
  }, [id, dispatch]);

  return (
    <div className="space-y-8 w-full max-w-4xl mx-auto">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="platform-detail-heading" data-cy="platformDetailsHeading">
            <Translate contentKey="platformManagement.detail.title">Platform Details</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="platformManagement.detail.subtitle">View platform information</Translate>
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/platform-management" replace>
              <ArrowLeft className="mr-2 h-4 w-4" />
              <Translate contentKey="entity.action.back">Back</Translate>
            </Link>
          </Button>
          <Button asChild variant="default">
            <Link to={`/admin/platform-management/${id}/edit`} replace>
              <Pencil className="mr-2 h-4 w-4" />
              <Translate contentKey="entity.action.edit">Edit</Translate>
            </Link>
          </Button>
        </div>
      </div>

      {loading && !platform ? (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : platform ? (
        <Card>
          <CardHeader>
            <CardTitle>
              <Translate contentKey="platformManagement.basicInfo">Basic Information</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="platformManagement.detail.description">Platform details and configuration</Translate>
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="global.field.id">ID</Translate>
                </Label>
                <p className="text-sm font-medium">{platform.id}</p>
              </div>
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="platformManagement.active">Status</Translate>
                </Label>
                <div>
                  {platform.active !== false ? (
                    <Badge variant="default">
                      <Translate contentKey="platformManagement.active">Active</Translate>
                    </Badge>
                  ) : (
                    <Badge variant="destructive">
                      <Translate contentKey="platformManagement.inactive">Inactive</Translate>
                    </Badge>
                  )}
                </div>
              </div>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-medium text-muted-foreground">
                <Translate contentKey="platformManagement.name">Name</Translate>
              </Label>
              <p className="text-sm">{platform.name}</p>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-medium text-muted-foreground">
                <Translate contentKey="platformManagement.description">Description</Translate>
              </Label>
              <p className="text-sm whitespace-pre-wrap">{platform.description || '-'}</p>
            </div>
            {platform.createdDate && (
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="entity.field.createdDate">Created Date</Translate>
                </Label>
                <p className="text-sm">{new Date(platform.createdDate).toLocaleString()}</p>
              </div>
            )}
            {platform.lastModifiedDate && (
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="entity.field.lastModifiedDate">Last Modified Date</Translate>
                </Label>
                <p className="text-sm">{new Date(platform.lastModifiedDate).toLocaleString()}</p>
              </div>
            )}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="pt-6">
            <p className="text-center text-muted-foreground">
              <Translate contentKey="platformManagement.detail.notFound">Platform not found</Translate>
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default PlatformManagementDetail;

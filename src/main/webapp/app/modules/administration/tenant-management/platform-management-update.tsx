import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Translate, translate, ValidatedField, ValidatedForm } from 'react-jhipster';
import { ArrowLeft, Save, Loader2 } from 'lucide-react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getPlatform, updatePlatform, createPlatform } from './platform.reducer';
import { IPlatform } from './platform.model';

export const PlatformManagementUpdate = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const platform = useAppSelector(state => state.platform.platform);
  const loading = useAppSelector(state => state.platform.loading);
  const updating = useAppSelector(state => state.platform.updating);
  const updateSuccess = useAppSelector(state => state.platform.updateSuccess);

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getPlatform(id));
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (updateSuccess) {
      navigate('/admin/platform-management');
    }
  }, [updateSuccess, navigate]);

  const saveEntity = (values: IPlatform) => {
    const entity = {
      ...platform,
      ...values,
      active: values.active !== false,
    };

    if (isNew) {
      dispatch(createPlatform(entity));
    } else {
      dispatch(updatePlatform(entity));
    }
  };

  const defaultValues = () => {
    return isNew
      ? {
          active: true,
          name: '',
          description: '',
        }
      : {
          ...platform,
          active: platform?.active !== false,
        };
  };

  return (
    <div className="space-y-8 w-full max-w-4xl mx-auto">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight" id="platformManagementUpdateHeading" data-cy="PlatformCreateUpdateHeading">
          <Translate contentKey="platformManagement.home.createOrEditLabel">Create or edit a Platform</Translate>
        </h1>
        <p className="text-muted-foreground mt-1.5">
          <Translate contentKey="platformManagement.home.subtitle">Manage platform type settings</Translate>
        </p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <ValidatedForm key={isNew ? 'new' : `edit-${platform?.id || id}`} defaultValues={defaultValues()} onSubmit={saveEntity}>
          {/* Basic Information Section */}
          <Card>
            <CardHeader>
              <CardTitle>
                <Translate contentKey="platformManagement.basicInfo">Basic Information</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="platformManagement.help.name">Enter the basic details for the platform</Translate>
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {!isNew ? (
                <div className="space-y-2">
                  <ValidatedField
                    name="id"
                    required
                    readOnly
                    id="platform-id"
                    label={translate('global.field.id')}
                    validate={{ required: true }}
                  />
                </div>
              ) : null}
              <div className="space-y-2">
                <ValidatedField
                  name="name"
                  label={translate('platformManagement.name')}
                  id="platform-name"
                  type="text"
                  validate={{
                    required: { value: true, message: translate('entity.validation.required') },
                    minLength: { value: 2, message: translate('entity.validation.minlength', { min: 2 }) },
                    maxLength: { value: 100, message: translate('entity.validation.maxlength', { max: 100 }) },
                  }}
                />
                <p className="text-sm text-muted-foreground">
                  <Translate contentKey="platformManagement.help.name">Enter the platform name</Translate>
                </p>
              </div>
              <div className="space-y-2">
                <ValidatedField
                  name="description"
                  label={translate('platformManagement.description')}
                  id="platform-description"
                  type="textarea"
                  rows={4}
                  validate={{
                    maxLength: { value: 500, message: translate('entity.validation.maxlength', { max: 500 }) },
                  }}
                />
                <p className="text-sm text-muted-foreground">
                  <Translate contentKey="platformManagement.help.description">Enter a description for the platform</Translate>
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Active Status */}
          <Card>
            <CardContent className="pt-6">
              <div className="space-y-2">
                <ValidatedField name="active" label={translate('platformManagement.active')} id="platform-active" check type="checkbox" />
              </div>
            </CardContent>
          </Card>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-4">
            <Button asChild id="cancel-save" data-cy="entityCreateCancelButton" variant="outline">
              <Link to="/admin/platform-management" replace>
                <ArrowLeft className="mr-2 h-4 w-4" />
                <Translate contentKey="entity.action.back">Back</Translate>
              </Link>
            </Button>
            <Button variant="default" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
              {updating ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
              <Translate contentKey="entity.action.save">Save</Translate>
            </Button>
          </div>
        </ValidatedForm>
      )}
    </div>
  );
};

export default PlatformManagementUpdate;

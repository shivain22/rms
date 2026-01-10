import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Translate, translate } from 'react-jhipster';
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

  // Local form state
  const [formData, setFormData] = useState<IPlatform>({
    name: '',
    description: '',
    active: true,
  });
  const [dataReady, setDataReady] = useState(false);

  // Fetch platform data when editing
  useEffect(() => {
    if (!isNew && id) {
      dispatch(getPlatform(id));
    } else if (isNew) {
      setDataReady(true);
    }
  }, [isNew, id, dispatch]);

  // Populate form when platform data is loaded
  useEffect(() => {
    if (!isNew && platform && platform.id && String(platform.id) === id) {
      setFormData({
        id: platform.id,
        name: platform.name || '',
        description: platform.description || '',
        active: platform.active !== false,
      });
      setDataReady(true);
    }
  }, [platform, isNew, id]);

  // Navigate away on successful update
  useEffect(() => {
    if (updateSuccess) {
      navigate('/admin/platform-management');
    }
  }, [updateSuccess, navigate]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const entity: IPlatform = {
      ...formData,
      id: isNew ? undefined : platform?.id,
    };

    if (isNew) {
      dispatch(createPlatform(entity));
    } else {
      dispatch(updatePlatform(entity));
    }
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

      {loading || !dataReady ? (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <form onSubmit={handleSubmit}>
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
              {!isNew && (
                <div className="space-y-2">
                  <Label htmlFor="platform-id">{translate('global.field.id')}</Label>
                  <Input id="platform-id" name="id" value={formData.id || ''} readOnly className="bg-muted" />
                </div>
              )}
              <div className="space-y-2">
                <Label htmlFor="platform-name">
                  {translate('platformManagement.name')} <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="platform-name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  required
                  minLength={2}
                  maxLength={100}
                />
                <p className="text-sm text-muted-foreground">
                  <Translate contentKey="platformManagement.help.name">Enter the platform name</Translate>
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="platform-description">{translate('platformManagement.description')}</Label>
                <textarea
                  id="platform-description"
                  name="description"
                  value={formData.description || ''}
                  onChange={handleInputChange}
                  rows={4}
                  maxLength={500}
                  className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                />
                <p className="text-sm text-muted-foreground">
                  <Translate contentKey="platformManagement.help.description">Enter a description for the platform</Translate>
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Active Status */}
          <Card className="mt-6">
            <CardContent className="pt-6">
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="platform-active"
                  checked={formData.active}
                  onCheckedChange={checked => setFormData(prev => ({ ...prev, active: !!checked }))}
                />
                <Label htmlFor="platform-active" className="cursor-pointer">
                  {translate('platformManagement.active')}
                </Label>
              </div>
            </CardContent>
          </Card>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-4 mt-6">
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
        </form>
      )}
    </div>
  );
};

export default PlatformManagementUpdate;

import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Translate, translate, ValidatedForm, ValidatedField } from 'react-jhipster';
import { ArrowLeft, Save, Loader2 } from 'lucide-react';
import { Checkbox } from '@/components/ui/checkbox';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDatabaseVersion, updateDatabaseVersion, createDatabaseVersion } from './database-version.reducer';
import { IDatabaseVersion } from './database-version.model';
import { getDatabases } from './database.reducer';

const DatabaseVersionManagementUpdate = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const isNew = id === undefined;
  const version = useAppSelector(state => state.databaseVersionManagement.databaseVersion);
  const loading = useAppSelector(state => state.databaseVersionManagement.loading);
  const databases = useAppSelector(state => state.databaseManagement.databases);

  const [formData, setFormData] = useState<IDatabaseVersion>({
    databaseId: undefined,
    version: '',
    displayName: '',
    releaseDate: '',
    endOfLifeDate: '',
    releaseNotes: '',
    isSupported: true,
    isRecommended: false,
    active: true,
  });

  useEffect(() => {
    dispatch(getDatabases());
  }, [dispatch]);

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getDatabaseVersion(id));
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (version && !isNew) {
      setFormData(version);
    }
  }, [version, isNew]);

  const handleSubmit = async (values: IDatabaseVersion) => {
    const entity = {
      ...values,
      id: isNew ? undefined : version?.id,
    };

    if (isNew) {
      await dispatch(createDatabaseVersion(entity));
    } else {
      await dispatch(updateDatabaseVersion(entity));
    }
    navigate('/admin/database-version-management');
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey={isNew ? 'databaseVersionManagement.home.createLabel' : 'databaseVersionManagement.home.editLabel'}>
              {isNew ? 'Create Database Version' : 'Edit Database Version'}
            </Translate>
          </h1>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/database-version-management">
            <ArrowLeft className="mr-2 h-4 w-4" />
            <Translate contentKey="entity.action.back">Back</Translate>
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>
            <Translate contentKey="databaseVersionManagement.form.title">Database Version Information</Translate>
          </CardTitle>
          <CardDescription>
            <Translate contentKey="databaseVersionManagement.form.description">Enter database version details</Translate>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ValidatedForm onSubmit={handleSubmit} defaultValues={formData}>
            <div className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="databaseId">
                  <Translate contentKey="databaseVersionManagement.database">Database</Translate>
                  <span className="text-destructive">*</span>
                </Label>
                <Select
                  value={formData.databaseId?.toString() || ''}
                  onValueChange={value => setFormData({ ...formData, databaseId: parseInt(value) })}
                >
                  <SelectTrigger id="databaseId">
                    <SelectValue placeholder="Select a database" />
                  </SelectTrigger>
                  <SelectContent>
                    {databases.map(database => (
                      <SelectItem key={database.id} value={database.id.toString()}>
                        {database.displayName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <ValidatedField
                name="version"
                label={translate('databaseVersionManagement.version')}
                required
                validate={{ required: 'This field is required.' }}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="version">
                      <Translate contentKey="databaseVersionManagement.version">Version</Translate>
                      <span className="text-destructive">*</span>
                    </Label>
                    <Input {...field} id="version" placeholder="e.g., 19c, 8.0, 14" />
                  </div>
                )}
              />

              <ValidatedField
                name="displayName"
                label={translate('databaseVersionManagement.displayName')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="displayName">
                      <Translate contentKey="databaseVersionManagement.displayName">Display Name</Translate>
                    </Label>
                    <Input {...field} id="displayName" placeholder="e.g., Oracle Database 19c" />
                  </div>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <ValidatedField
                  name="releaseDate"
                  label={translate('databaseVersionManagement.releaseDate')}
                  type="date"
                  render={({ field }) => (
                    <div className="space-y-2">
                      <Label htmlFor="releaseDate">
                        <Translate contentKey="databaseVersionManagement.releaseDate">Release Date</Translate>
                      </Label>
                      <Input {...field} id="releaseDate" type="date" />
                    </div>
                  )}
                />

                <ValidatedField
                  name="endOfLifeDate"
                  label={translate('databaseVersionManagement.endOfLifeDate')}
                  type="date"
                  render={({ field }) => (
                    <div className="space-y-2">
                      <Label htmlFor="endOfLifeDate">
                        <Translate contentKey="databaseVersionManagement.endOfLifeDate">End of Life Date</Translate>
                      </Label>
                      <Input {...field} id="endOfLifeDate" type="date" />
                    </div>
                  )}
                />
              </div>

              <ValidatedField
                name="releaseNotes"
                label={translate('databaseVersionManagement.releaseNotes')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="releaseNotes">
                      <Translate contentKey="databaseVersionManagement.releaseNotes">Release Notes</Translate>
                    </Label>
                    <Input {...field} id="releaseNotes" placeholder="Release notes and updates" />
                  </div>
                )}
              />

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="isSupported"
                  checked={formData.isSupported}
                  onCheckedChange={checked => setFormData({ ...formData, isSupported: !!checked })}
                />
                <Label htmlFor="isSupported" className="cursor-pointer">
                  <Translate contentKey="databaseVersionManagement.isSupported">Supported</Translate>
                </Label>
              </div>

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="isRecommended"
                  checked={formData.isRecommended}
                  onCheckedChange={checked => setFormData({ ...formData, isRecommended: !!checked })}
                />
                <Label htmlFor="isRecommended" className="cursor-pointer">
                  <Translate contentKey="databaseVersionManagement.isRecommended">Recommended</Translate>
                </Label>
              </div>

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="active"
                  checked={formData.active}
                  onCheckedChange={checked => setFormData({ ...formData, active: !!checked })}
                />
                <Label htmlFor="active" className="cursor-pointer">
                  <Translate contentKey="databaseVersionManagement.active">Active</Translate>
                </Label>
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button type="button" variant="outline" asChild>
                  <Link to="/admin/database-version-management">
                    <Translate contentKey="entity.action.cancel">Cancel</Translate>
                  </Link>
                </Button>
                <Button type="submit" disabled={loading}>
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      <Translate contentKey="entity.action.saving">Saving...</Translate>
                    </>
                  ) : (
                    <>
                      <Save className="mr-2 h-4 w-4" />
                      <Translate contentKey="entity.action.save">Save</Translate>
                    </>
                  )}
                </Button>
              </div>
            </div>
          </ValidatedForm>
        </CardContent>
      </Card>
    </div>
  );
};

export default DatabaseVersionManagementUpdate;

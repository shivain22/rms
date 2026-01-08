import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
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
  const [submitting, setSubmitting] = useState(false);
  const [dataReady, setDataReady] = useState(false);

  useEffect(() => {
    dispatch(getDatabases());
  }, [dispatch]);

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getDatabaseVersion(id));
    } else if (isNew) {
      setDataReady(true);
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (version && !isNew) {
      setFormData({
        id: version.id,
        databaseId: version.databaseId,
        version: version.version || '',
        displayName: version.displayName || '',
        releaseDate: version.releaseDate || '',
        endOfLifeDate: version.endOfLifeDate || '',
        releaseNotes: version.releaseNotes || '',
        isSupported: version.isSupported !== undefined ? version.isSupported : true,
        isRecommended: version.isRecommended !== undefined ? version.isRecommended : false,
        active: version.active !== undefined ? version.active : true,
      });
      setDataReady(true);
    }
  }, [version, isNew]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const entity = {
        ...formData,
        id: isNew ? undefined : version?.id,
      };

      if (isNew) {
        await dispatch(createDatabaseVersion(entity));
      } else {
        await dispatch(updateDatabaseVersion(entity));
      }
      navigate('/admin/database-version-management');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{isNew ? 'Create Database Version' : 'Edit Database Version'}</h1>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/database-version-management">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Database Version Information</CardTitle>
          <CardDescription>Enter database version details</CardDescription>
        </CardHeader>
        <CardContent>
          {!dataReady ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin" />
              <span className="ml-2">Loading...</span>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="space-y-6">
                <div className="space-y-2">
                  <Label htmlFor="databaseId">
                    Database <span className="text-destructive">*</span>
                  </Label>
                  <Select
                    value={formData.databaseId?.toString() || ''}
                    onValueChange={value => setFormData({ ...formData, databaseId: parseInt(value, 10) })}
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

                <div className="space-y-2">
                  <Label htmlFor="version">
                    Version <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="version"
                    name="version"
                    value={formData.version}
                    onChange={handleInputChange}
                    placeholder="e.g., 19c, 8.0, 14"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="displayName">Display Name</Label>
                  <Input
                    id="displayName"
                    name="displayName"
                    value={formData.displayName}
                    onChange={handleInputChange}
                    placeholder="e.g., Oracle Database 19c"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="releaseDate">Release Date</Label>
                    <Input id="releaseDate" name="releaseDate" type="date" value={formData.releaseDate} onChange={handleInputChange} />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="endOfLifeDate">End of Life Date</Label>
                    <Input
                      id="endOfLifeDate"
                      name="endOfLifeDate"
                      type="date"
                      value={formData.endOfLifeDate}
                      onChange={handleInputChange}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="releaseNotes">Release Notes</Label>
                  <Input
                    id="releaseNotes"
                    name="releaseNotes"
                    value={formData.releaseNotes}
                    onChange={handleInputChange}
                    placeholder="Release notes and updates"
                  />
                </div>

                <div className="flex items-center space-x-2">
                  <Checkbox
                    id="isSupported"
                    checked={formData.isSupported}
                    onCheckedChange={checked => setFormData({ ...formData, isSupported: !!checked })}
                  />
                  <Label htmlFor="isSupported" className="cursor-pointer">
                    Supported
                  </Label>
                </div>

                <div className="flex items-center space-x-2">
                  <Checkbox
                    id="isRecommended"
                    checked={formData.isRecommended}
                    onCheckedChange={checked => setFormData({ ...formData, isRecommended: !!checked })}
                  />
                  <Label htmlFor="isRecommended" className="cursor-pointer">
                    Recommended
                  </Label>
                </div>

                <div className="flex items-center space-x-2">
                  <Checkbox
                    id="active"
                    checked={formData.active}
                    onCheckedChange={checked => setFormData({ ...formData, active: !!checked })}
                  />
                  <Label htmlFor="active" className="cursor-pointer">
                    Active
                  </Label>
                </div>

                <div className="flex justify-end gap-2 pt-4">
                  <Button type="button" variant="outline" asChild>
                    <Link to="/admin/database-version-management">Cancel</Link>
                  </Button>
                  <Button type="submit" disabled={submitting || (!isNew && !version)}>
                    {submitting ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Saving...
                      </>
                    ) : (
                      <>
                        <Save className="mr-2 h-4 w-4" />
                        Save
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default DatabaseVersionManagementUpdate;

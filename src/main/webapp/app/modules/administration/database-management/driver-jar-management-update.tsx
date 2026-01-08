import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ArrowLeft, Save, Loader2, Upload } from 'lucide-react';
import { Checkbox } from '@/components/ui/checkbox';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDriverJar, updateDriverJar, uploadDriverJar } from './driver-jar.reducer';
import { IDriverJar } from './driver-jar.model';
import { getDatabaseVersions } from './database-version.reducer';

const DriverJarManagementUpdate = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const isNew = id === undefined;
  const driver = useAppSelector(state => state.driverJarManagement.driverJar);
  const loading = useAppSelector(state => state.driverJarManagement.loading);
  const versions = useAppSelector(state => state.databaseVersionManagement.databaseVersions);

  const [formData, setFormData] = useState<IDriverJar>({
    versionId: undefined,
    driverType: 'JDBC',
    driverClassName: '',
    description: '',
    isDefault: false,
    active: true,
  });

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    dispatch(getDatabaseVersions());
  }, [dispatch]);

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getDriverJar(id));
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (driver && !isNew) {
      setFormData(driver);
    }
  }, [driver, isNew]);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files[0]) {
      setSelectedFile(event.target.files[0]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (isNew) {
      // Upload new driver
      if (!selectedFile || !formData.versionId || !formData.driverClassName) {
        alert('Please fill in all required fields and select a file');
        return;
      }

      setUploading(true);
      try {
        await dispatch(
          uploadDriverJar({
            versionId: formData.versionId,
            driverType: formData.driverType,
            driverClassName: formData.driverClassName,
            file: selectedFile,
            description: formData.description,
          }),
        );
        navigate('/admin/driver-jar-management');
      } catch (error) {
        console.error('Error uploading driver:', error);
        alert('Failed to upload driver. Please try again.');
      } finally {
        setUploading(false);
      }
    } else {
      // Update existing driver
      const entity = {
        ...formData,
        id: driver?.id,
      };
      await dispatch(updateDriverJar(entity));
      navigate('/admin/driver-jar-management');
    }
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{isNew ? 'Upload Driver JAR' : 'Edit Driver JAR'}</h1>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/driver-jar-management">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Driver JAR Information</CardTitle>
          <CardDescription>{isNew ? 'Upload a JDBC or R2DBC driver JAR file' : 'Update driver JAR details'}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit}>
            <div className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="versionId">
                  Database Version <span className="text-destructive">*</span>
                </Label>
                <Select
                  value={formData.versionId?.toString() || ''}
                  onValueChange={value => setFormData({ ...formData, versionId: parseInt(value, 10) })}
                  disabled={!isNew}
                >
                  <SelectTrigger id="versionId">
                    <SelectValue placeholder="Select a database version" />
                  </SelectTrigger>
                  <SelectContent>
                    {versions.map(version => (
                      <SelectItem key={version.id} value={version.id.toString()}>
                        {version.displayName || version.version}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="driverType">
                  Driver Type <span className="text-destructive">*</span>
                </Label>
                <Select
                  value={formData.driverType || 'JDBC'}
                  onValueChange={value => setFormData({ ...formData, driverType: value })}
                  disabled={!isNew}
                >
                  <SelectTrigger id="driverType">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="JDBC">JDBC</SelectItem>
                    <SelectItem value="R2DBC">R2DBC</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {isNew && (
                <div className="space-y-2">
                  <Label htmlFor="file">
                    JAR File <span className="text-destructive">*</span>
                  </Label>
                  <Input id="file" type="file" accept=".jar" onChange={handleFileChange} required={isNew} className="cursor-pointer" />
                  {selectedFile && (
                    <p className="text-sm text-muted-foreground">
                      Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(2)} KB)
                    </p>
                  )}
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="driverClassName">
                  Driver Class Name <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="driverClassName"
                  value={formData.driverClassName}
                  onChange={e => setFormData({ ...formData, driverClassName: e.target.value })}
                  placeholder="e.g., org.postgresql.Driver"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Input
                  id="description"
                  value={formData.description}
                  onChange={e => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Driver description"
                />
              </div>

              {!isNew && (
                <>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="isDefault"
                      checked={formData.isDefault}
                      onCheckedChange={checked => setFormData({ ...formData, isDefault: !!checked })}
                    />
                    <Label htmlFor="isDefault" className="cursor-pointer">
                      Set as Default
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
                </>
              )}

              <div className="flex justify-end gap-2 pt-4">
                <Button type="button" variant="outline" asChild>
                  <Link to="/admin/driver-jar-management">Cancel</Link>
                </Button>
                <Button type="submit" disabled={loading || uploading}>
                  {loading || uploading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      {isNew ? 'Uploading...' : 'Saving...'}
                    </>
                  ) : (
                    <>
                      {isNew ? (
                        <>
                          <Upload className="mr-2 h-4 w-4" />
                          Upload
                        </>
                      ) : (
                        <>
                          <Save className="mr-2 h-4 w-4" />
                          Save
                        </>
                      )}
                    </>
                  )}
                </Button>
              </div>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default DriverJarManagementUpdate;

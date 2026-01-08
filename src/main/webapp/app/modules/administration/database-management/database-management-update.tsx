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
import { getDatabase, updateDatabase, createDatabase } from './database.reducer';
import { IDatabase } from './database.model';
import { getDatabaseVendors } from './database-vendor.reducer';

const DatabaseManagementUpdate = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const isNew = id === undefined;
  const database = useAppSelector(state => state.databaseManagement.database);
  const loading = useAppSelector(state => state.databaseManagement.loading);
  const vendors = useAppSelector(state => state.databaseVendorManagement.databaseVendors);

  const [formData, setFormData] = useState<IDatabase>({
    vendorId: undefined,
    databaseCode: '',
    displayName: '',
    description: '',
    defaultDriverClassName: '',
    defaultPort: 5432,
    jdbcUrlTemplate: '',
    r2dbcUrlTemplate: '',
    active: true,
  });
  const [submitting, setSubmitting] = useState(false);
  const [dataReady, setDataReady] = useState(false);

  useEffect(() => {
    dispatch(getDatabaseVendors());
  }, [dispatch]);

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getDatabase(id));
    } else if (isNew) {
      setDataReady(true);
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (database && !isNew) {
      setFormData({
        id: database.id,
        vendorId: database.vendorId,
        databaseCode: database.databaseCode || '',
        displayName: database.displayName || '',
        description: database.description || '',
        defaultDriverClassName: database.defaultDriverClassName || '',
        defaultPort: database.defaultPort || 5432,
        jdbcUrlTemplate: database.jdbcUrlTemplate || '',
        r2dbcUrlTemplate: database.r2dbcUrlTemplate || '',
        active: database.active !== undefined ? database.active : true,
      });
      setDataReady(true);
    }
  }, [database, isNew]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'number' ? parseInt(value, 10) || 0 : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const entity = {
        ...formData,
        id: isNew ? undefined : database?.id,
      };

      if (isNew) {
        await dispatch(createDatabase(entity));
      } else {
        await dispatch(updateDatabase(entity));
      }
      navigate('/admin/database-management');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{isNew ? 'Create Database' : 'Edit Database'}</h1>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/database-management">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Database Information</CardTitle>
          <CardDescription>Enter database details</CardDescription>
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
                  <Label htmlFor="vendorId">
                    Vendor <span className="text-destructive">*</span>
                  </Label>
                  <Select
                    value={formData.vendorId?.toString() || ''}
                    onValueChange={value => setFormData({ ...formData, vendorId: parseInt(value, 10) })}
                  >
                    <SelectTrigger id="vendorId">
                      <SelectValue placeholder="Select a vendor" />
                    </SelectTrigger>
                    <SelectContent>
                      {vendors.map(vendor => (
                        <SelectItem key={vendor.id} value={vendor.id.toString()}>
                          {vendor.displayName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="databaseCode">
                    Database Code <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="databaseCode"
                    name="databaseCode"
                    value={formData.databaseCode}
                    onChange={handleInputChange}
                    placeholder="e.g., ORACLE_DB, MYSQL, POSTGRESQL"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="displayName">
                    Display Name <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="displayName"
                    name="displayName"
                    value={formData.displayName}
                    onChange={handleInputChange}
                    placeholder="e.g., Oracle Database, MySQL, PostgreSQL"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="defaultPort">Default Port</Label>
                    <Input
                      id="defaultPort"
                      name="defaultPort"
                      type="number"
                      value={formData.defaultPort}
                      onChange={handleInputChange}
                      placeholder="5432"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="defaultDriverClassName">Driver Class Name</Label>
                    <Input
                      id="defaultDriverClassName"
                      name="defaultDriverClassName"
                      value={formData.defaultDriverClassName}
                      onChange={handleInputChange}
                      placeholder="e.g., org.postgresql.Driver"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Description</Label>
                  <Input
                    id="description"
                    name="description"
                    value={formData.description}
                    onChange={handleInputChange}
                    placeholder="Database description"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="jdbcUrlTemplate">JDBC URL Template</Label>
                  <Input
                    id="jdbcUrlTemplate"
                    name="jdbcUrlTemplate"
                    value={formData.jdbcUrlTemplate}
                    onChange={handleInputChange}
                    placeholder="e.g., jdbc:postgresql://{host}:{port}/{database}"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="r2dbcUrlTemplate">R2DBC URL Template</Label>
                  <Input
                    id="r2dbcUrlTemplate"
                    name="r2dbcUrlTemplate"
                    value={formData.r2dbcUrlTemplate}
                    onChange={handleInputChange}
                    placeholder="e.g., r2dbc:postgresql://{host}:{port}/{database}"
                  />
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
                    <Link to="/admin/database-management">Cancel</Link>
                  </Button>
                  <Button type="submit" disabled={submitting || (!isNew && !database)}>
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

export default DatabaseManagementUpdate;

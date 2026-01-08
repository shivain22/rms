import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Save, Loader2 } from 'lucide-react';
import { Checkbox } from '@/components/ui/checkbox';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDatabaseVendor, updateDatabaseVendor, createDatabaseVendor } from './database-vendor.reducer';
import { IDatabaseVendor } from './database-vendor.model';

const DatabaseVendorManagementUpdate = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const isNew = id === undefined;
  const vendor = useAppSelector(state => state.databaseVendorManagement.databaseVendor);
  const loading = useAppSelector(state => state.databaseVendorManagement.loading);

  const [formData, setFormData] = useState<IDatabaseVendor>({
    vendorCode: '',
    displayName: '',
    defaultPort: 5432,
    driverKey: '',
    description: '',
    jdbcUrlTemplate: '',
    r2dbcUrlTemplate: '',
    driverClassName: '',
    active: true,
  });
  const [submitting, setSubmitting] = useState(false);
  const [dataReady, setDataReady] = useState(false);

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getDatabaseVendor(id));
    } else if (isNew) {
      setDataReady(true);
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (vendor && !isNew) {
      setFormData({
        id: vendor.id,
        vendorCode: vendor.vendorCode || '',
        displayName: vendor.displayName || '',
        defaultPort: vendor.defaultPort || 5432,
        driverKey: vendor.driverKey || '',
        description: vendor.description || '',
        jdbcUrlTemplate: vendor.jdbcUrlTemplate || '',
        r2dbcUrlTemplate: vendor.r2dbcUrlTemplate || '',
        driverClassName: vendor.driverClassName || '',
        active: vendor.active !== undefined ? vendor.active : true,
      });
      setDataReady(true);
    }
  }, [vendor, isNew]);

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
        id: isNew ? undefined : vendor?.id,
      };

      if (isNew) {
        await dispatch(createDatabaseVendor(entity));
      } else {
        await dispatch(updateDatabaseVendor(entity));
      }
      navigate('/admin/database-vendor-management');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{isNew ? 'Create Database Vendor' : 'Edit Database Vendor'}</h1>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/database-vendor-management">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Database Vendor Information</CardTitle>
          <CardDescription>Enter database vendor details</CardDescription>
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
                  <Label htmlFor="vendorCode">
                    Vendor Code <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="vendorCode"
                    name="vendorCode"
                    value={formData.vendorCode}
                    onChange={handleInputChange}
                    placeholder="e.g., POSTGRESQL, MYSQL, ORACLE"
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
                    placeholder="e.g., PostgreSQL, MySQL, Oracle"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="defaultPort">
                      Default Port <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="defaultPort"
                      name="defaultPort"
                      type="number"
                      value={formData.defaultPort}
                      onChange={handleInputChange}
                      placeholder="5432"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="driverKey">
                      Driver Key <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="driverKey"
                      name="driverKey"
                      value={formData.driverKey}
                      onChange={handleInputChange}
                      placeholder="e.g., postgresql, mysql, oracle"
                      required
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
                    placeholder="Database vendor description"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="driverClassName">Driver Class Name</Label>
                  <Input
                    id="driverClassName"
                    name="driverClassName"
                    value={formData.driverClassName}
                    onChange={handleInputChange}
                    placeholder="e.g., org.postgresql.Driver"
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
                    <Link to="/admin/database-vendor-management">Cancel</Link>
                  </Button>
                  <Button type="submit" disabled={submitting || (!isNew && !vendor)}>
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

export default DatabaseVendorManagementUpdate;

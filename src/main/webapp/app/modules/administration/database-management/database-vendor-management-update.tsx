import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Translate, translate, ValidatedForm, ValidatedField } from 'react-jhipster';
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

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getDatabaseVendor(id));
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (vendor && !isNew) {
      setFormData(vendor);
    }
  }, [vendor, isNew]);

  const handleSubmit = async (values: IDatabaseVendor) => {
    const entity = {
      ...values,
      id: isNew ? undefined : vendor?.id,
    };

    if (isNew) {
      await dispatch(createDatabaseVendor(entity));
    } else {
      await dispatch(updateDatabaseVendor(entity));
    }
    navigate('/admin/database-vendor-management');
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey={isNew ? 'databaseVendorManagement.home.createLabel' : 'databaseVendorManagement.home.editLabel'}>
              {isNew ? 'Create Database Vendor' : 'Edit Database Vendor'}
            </Translate>
          </h1>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/database-vendor-management">
            <ArrowLeft className="mr-2 h-4 w-4" />
            <Translate contentKey="entity.action.back">Back</Translate>
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>
            <Translate contentKey="databaseVendorManagement.form.title">Database Vendor Information</Translate>
          </CardTitle>
          <CardDescription>
            <Translate contentKey="databaseVendorManagement.form.description">Enter database vendor details</Translate>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ValidatedForm onSubmit={handleSubmit} defaultValues={formData}>
            <div className="space-y-6">
              <ValidatedField
                name="vendorCode"
                label={translate('databaseVendorManagement.vendorCode')}
                required
                validate={{ required: 'This field is required.' }}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="vendorCode">
                      <Translate contentKey="databaseVendorManagement.vendorCode">Vendor Code</Translate>
                      <span className="text-destructive">*</span>
                    </Label>
                    <Input {...field} id="vendorCode" placeholder="e.g., POSTGRESQL, MYSQL, ORACLE" />
                  </div>
                )}
              />

              <ValidatedField
                name="displayName"
                label={translate('databaseVendorManagement.displayName')}
                required
                validate={{ required: 'This field is required.' }}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="displayName">
                      <Translate contentKey="databaseVendorManagement.displayName">Display Name</Translate>
                      <span className="text-destructive">*</span>
                    </Label>
                    <Input {...field} id="displayName" placeholder="e.g., PostgreSQL, MySQL, Oracle" />
                  </div>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <ValidatedField
                  name="defaultPort"
                  label={translate('databaseVendorManagement.defaultPort')}
                  required
                  type="number"
                  validate={{ required: 'This field is required.' }}
                  render={({ field }) => (
                    <div className="space-y-2">
                      <Label htmlFor="defaultPort">
                        <Translate contentKey="databaseVendorManagement.defaultPort">Default Port</Translate>
                        <span className="text-destructive">*</span>
                      </Label>
                      <Input {...field} id="defaultPort" type="number" placeholder="5432" />
                    </div>
                  )}
                />

                <ValidatedField
                  name="driverKey"
                  label={translate('databaseVendorManagement.driverKey')}
                  required
                  validate={{ required: 'This field is required.' }}
                  render={({ field }) => (
                    <div className="space-y-2">
                      <Label htmlFor="driverKey">
                        <Translate contentKey="databaseVendorManagement.driverKey">Driver Key</Translate>
                        <span className="text-destructive">*</span>
                      </Label>
                      <Input {...field} id="driverKey" placeholder="e.g., postgresql, mysql, oracle" />
                    </div>
                  )}
                />
              </div>

              <ValidatedField
                name="description"
                label={translate('databaseVendorManagement.description')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="description">
                      <Translate contentKey="databaseVendorManagement.description">Description</Translate>
                    </Label>
                    <Input {...field} id="description" placeholder="Database vendor description" />
                  </div>
                )}
              />

              <ValidatedField
                name="driverClassName"
                label={translate('databaseVendorManagement.driverClassName')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="driverClassName">
                      <Translate contentKey="databaseVendorManagement.driverClassName">Driver Class Name</Translate>
                    </Label>
                    <Input {...field} id="driverClassName" placeholder="e.g., org.postgresql.Driver" />
                  </div>
                )}
              />

              <ValidatedField
                name="jdbcUrlTemplate"
                label={translate('databaseVendorManagement.jdbcUrlTemplate')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="jdbcUrlTemplate">
                      <Translate contentKey="databaseVendorManagement.jdbcUrlTemplate">JDBC URL Template</Translate>
                    </Label>
                    <Input {...field} id="jdbcUrlTemplate" placeholder="e.g., jdbc:postgresql://{host}:{port}/{database}" />
                  </div>
                )}
              />

              <ValidatedField
                name="r2dbcUrlTemplate"
                label={translate('databaseVendorManagement.r2dbcUrlTemplate')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="r2dbcUrlTemplate">
                      <Translate contentKey="databaseVendorManagement.r2dbcUrlTemplate">R2DBC URL Template</Translate>
                    </Label>
                    <Input {...field} id="r2dbcUrlTemplate" placeholder="e.g., r2dbc:postgresql://{host}:{port}/{database}" />
                  </div>
                )}
              />

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="active"
                  checked={formData.active}
                  onCheckedChange={checked => setFormData({ ...formData, active: !!checked })}
                />
                <Label htmlFor="active" className="cursor-pointer">
                  <Translate contentKey="databaseVendorManagement.active">Active</Translate>
                </Label>
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button type="button" variant="outline" asChild>
                  <Link to="/admin/database-vendor-management">
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

export default DatabaseVendorManagementUpdate;

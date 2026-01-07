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

  useEffect(() => {
    dispatch(getDatabaseVendors());
  }, [dispatch]);

  useEffect(() => {
    if (!isNew && id) {
      dispatch(getDatabase(id));
    }
  }, [isNew, id, dispatch]);

  useEffect(() => {
    if (database && !isNew) {
      setFormData(database);
    }
  }, [database, isNew]);

  const handleSubmit = async (values: IDatabase) => {
    const entity = {
      ...values,
      id: isNew ? undefined : database?.id,
    };

    if (isNew) {
      await dispatch(createDatabase(entity));
    } else {
      await dispatch(updateDatabase(entity));
    }
    navigate('/admin/database-management');
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey={isNew ? 'databaseManagement.home.createLabel' : 'databaseManagement.home.editLabel'}>
              {isNew ? 'Create Database' : 'Edit Database'}
            </Translate>
          </h1>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/database-management">
            <ArrowLeft className="mr-2 h-4 w-4" />
            <Translate contentKey="entity.action.back">Back</Translate>
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>
            <Translate contentKey="databaseManagement.form.title">Database Information</Translate>
          </CardTitle>
          <CardDescription>
            <Translate contentKey="databaseManagement.form.description">Enter database details</Translate>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ValidatedForm onSubmit={handleSubmit} defaultValues={formData}>
            <div className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="vendorId">
                  <Translate contentKey="databaseManagement.vendor">Vendor</Translate>
                  <span className="text-destructive">*</span>
                </Label>
                <Select
                  value={formData.vendorId?.toString() || ''}
                  onValueChange={value => setFormData({ ...formData, vendorId: parseInt(value) })}
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

              <ValidatedField
                name="databaseCode"
                label={translate('databaseManagement.databaseCode')}
                required
                validate={{ required: 'This field is required.' }}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="databaseCode">
                      <Translate contentKey="databaseManagement.databaseCode">Database Code</Translate>
                      <span className="text-destructive">*</span>
                    </Label>
                    <Input {...field} id="databaseCode" placeholder="e.g., ORACLE_DB, MYSQL, POSTGRESQL" />
                  </div>
                )}
              />

              <ValidatedField
                name="displayName"
                label={translate('databaseManagement.displayName')}
                required
                validate={{ required: 'This field is required.' }}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="displayName">
                      <Translate contentKey="databaseManagement.displayName">Display Name</Translate>
                      <span className="text-destructive">*</span>
                    </Label>
                    <Input {...field} id="displayName" placeholder="e.g., Oracle Database, MySQL, PostgreSQL" />
                  </div>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <ValidatedField
                  name="defaultPort"
                  label={translate('databaseManagement.defaultPort')}
                  type="number"
                  render={({ field }) => (
                    <div className="space-y-2">
                      <Label htmlFor="defaultPort">
                        <Translate contentKey="databaseManagement.defaultPort">Default Port</Translate>
                      </Label>
                      <Input {...field} id="defaultPort" type="number" placeholder="5432" />
                    </div>
                  )}
                />

                <ValidatedField
                  name="defaultDriverClassName"
                  label={translate('databaseManagement.defaultDriverClassName')}
                  render={({ field }) => (
                    <div className="space-y-2">
                      <Label htmlFor="defaultDriverClassName">
                        <Translate contentKey="databaseManagement.defaultDriverClassName">Driver Class Name</Translate>
                      </Label>
                      <Input {...field} id="defaultDriverClassName" placeholder="e.g., org.postgresql.Driver" />
                    </div>
                  )}
                />
              </div>

              <ValidatedField
                name="description"
                label={translate('databaseManagement.description')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="description">
                      <Translate contentKey="databaseManagement.description">Description</Translate>
                    </Label>
                    <Input {...field} id="description" placeholder="Database description" />
                  </div>
                )}
              />

              <ValidatedField
                name="jdbcUrlTemplate"
                label={translate('databaseManagement.jdbcUrlTemplate')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="jdbcUrlTemplate">
                      <Translate contentKey="databaseManagement.jdbcUrlTemplate">JDBC URL Template</Translate>
                    </Label>
                    <Input {...field} id="jdbcUrlTemplate" placeholder="e.g., jdbc:postgresql://{host}:{port}/{database}" />
                  </div>
                )}
              />

              <ValidatedField
                name="r2dbcUrlTemplate"
                label={translate('databaseManagement.r2dbcUrlTemplate')}
                render={({ field }) => (
                  <div className="space-y-2">
                    <Label htmlFor="r2dbcUrlTemplate">
                      <Translate contentKey="databaseManagement.r2dbcUrlTemplate">R2DBC URL Template</Translate>
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
                  <Translate contentKey="databaseManagement.active">Active</Translate>
                </Label>
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button type="button" variant="outline" asChild>
                  <Link to="/admin/database-management">
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

export default DatabaseManagementUpdate;

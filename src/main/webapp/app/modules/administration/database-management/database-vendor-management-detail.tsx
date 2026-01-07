import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Translate } from 'react-jhipster';
import { ArrowLeft, Pencil } from 'lucide-react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDatabaseVendor } from './database-vendor.reducer';

export const DatabaseVendorManagementDetail = () => {
  const dispatch = useAppDispatch();
  const { id } = useParams<'id'>();

  const vendor = useAppSelector(state => state.databaseVendorManagement.databaseVendor);
  const loading = useAppSelector(state => state.databaseVendorManagement.loading);

  useEffect(() => {
    if (id) {
      dispatch(getDatabaseVendor(id));
    }
  }, [id]);

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey="databaseVendorManagement.detail.title">Database Vendor</Translate>
            {vendor && <span className="ml-2 text-muted-foreground">[{vendor.vendorCode}]</span>}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/database-vendor-management">
              <ArrowLeft className="mr-2 h-4 w-4" />
              <Translate contentKey="entity.action.back">Back</Translate>
            </Link>
          </Button>
          {vendor && (
            <Button asChild variant="default">
              <Link to={`/admin/database-vendor-management/${vendor.id}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                <Translate contentKey="entity.action.edit">Edit</Translate>
              </Link>
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">Loading...</div>
      ) : vendor ? (
        <Card>
          <CardHeader>
            <CardTitle>
              <Translate contentKey="databaseVendorManagement.detail.information">Vendor Information</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="databaseVendorManagement.detail.description">View database vendor details</Translate>
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="global.field.id">ID</Translate>
                </label>
                <p className="text-base">{vendor.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVendorManagement.vendorCode">Vendor Code</Translate>
                </label>
                <p className="text-base font-medium">{vendor.vendorCode}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVendorManagement.displayName">Display Name</Translate>
                </label>
                <p className="text-base">{vendor.displayName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVendorManagement.defaultPort">Default Port</Translate>
                </label>
                <p className="text-base">{vendor.defaultPort}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVendorManagement.driverKey">Driver Key</Translate>
                </label>
                <p className="text-base">{vendor.driverKey}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  <Translate contentKey="databaseVendorManagement.active">Status</Translate>
                </label>
                <p className="text-base">
                  <Badge variant={vendor.active ? 'default' : 'secondary'}>{vendor.active ? 'Active' : 'Inactive'}</Badge>
                </p>
              </div>
              {vendor.description && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="databaseVendorManagement.description">Description</Translate>
                  </label>
                  <p className="text-base">{vendor.description}</p>
                </div>
              )}
              {vendor.jdbcUrlTemplate && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="databaseVendorManagement.jdbcUrlTemplate">JDBC URL Template</Translate>
                  </label>
                  <p className="text-base font-mono text-sm">{vendor.jdbcUrlTemplate}</p>
                </div>
              )}
              {vendor.r2dbcUrlTemplate && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="databaseVendorManagement.r2dbcUrlTemplate">R2DBC URL Template</Translate>
                  </label>
                  <p className="text-base font-mono text-sm">{vendor.r2dbcUrlTemplate}</p>
                </div>
              )}
              {vendor.driverClassName && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="databaseVendorManagement.driverClassName">Driver Class Name</Translate>
                  </label>
                  <p className="text-base font-mono text-sm">{vendor.driverClassName}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            <Translate contentKey="databaseVendorManagement.detail.notFound">Database vendor not found</Translate>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default DatabaseVendorManagementDetail;

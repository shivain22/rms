import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
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
            Database Vendor
            {vendor && <span className="ml-2 text-muted-foreground">[{vendor.vendorCode}]</span>}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/database-vendor-management">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back
            </Link>
          </Button>
          {vendor && (
            <Button asChild variant="default">
              <Link to={`/admin/database-vendor-management/${vendor.id}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                Edit
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
            <CardTitle>Vendor Information</CardTitle>
            <CardDescription>View database vendor details</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">ID</label>
                <p className="text-base">{vendor.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Vendor Code</label>
                <p className="text-base font-medium">{vendor.vendorCode}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Display Name</label>
                <p className="text-base">{vendor.displayName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Default Port</label>
                <p className="text-base">{vendor.defaultPort}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Driver Key</label>
                <p className="text-base">{vendor.driverKey}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Status</label>
                <p className="text-base">
                  <Badge variant={vendor.active ? 'default' : 'secondary'}>{vendor.active ? 'Active' : 'Inactive'}</Badge>
                </p>
              </div>
              {vendor.description && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">Description</label>
                  <p className="text-base">{vendor.description}</p>
                </div>
              )}
              {vendor.jdbcUrlTemplate && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">JDBC URL Template</label>
                  <p className="text-base font-mono text-sm">{vendor.jdbcUrlTemplate}</p>
                </div>
              )}
              {vendor.r2dbcUrlTemplate && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">R2DBC URL Template</label>
                  <p className="text-base font-mono text-sm">{vendor.r2dbcUrlTemplate}</p>
                </div>
              )}
              {vendor.driverClassName && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">Driver Class Name</label>
                  <p className="text-base font-mono text-sm">{vendor.driverClassName}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">Database vendor not found</CardContent>
        </Card>
      )}
    </div>
  );
};

export default DatabaseVendorManagementDetail;

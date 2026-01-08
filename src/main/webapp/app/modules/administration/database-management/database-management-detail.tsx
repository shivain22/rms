import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Pencil } from 'lucide-react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getDatabase } from './database.reducer';
import { getDatabaseVendors } from './database-vendor.reducer';

export const DatabaseManagementDetail = () => {
  const dispatch = useAppDispatch();
  const { id } = useParams<'id'>();

  const database = useAppSelector(state => state.databaseManagement.database);
  const loading = useAppSelector(state => state.databaseManagement.loading);
  const vendors = useAppSelector(state => state.databaseVendorManagement.databaseVendors);

  useEffect(() => {
    if (id) {
      dispatch(getDatabase(id));
    }
    dispatch(getDatabaseVendors());
  }, [id]);

  const getVendorName = (vendorId: number) => {
    const vendor = vendors.find(v => v.id === vendorId);
    return vendor?.displayName || `Vendor ${vendorId}`;
  };

  return (
    <div className="space-y-8 w-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Database
            {database && <span className="ml-2 text-muted-foreground">[{database.databaseCode}]</span>}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/database-management">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back
            </Link>
          </Button>
          {database && (
            <Button asChild variant="default">
              <Link to={`/admin/database-management/${database.id}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                Edit
              </Link>
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">Loading...</div>
      ) : database ? (
        <Card>
          <CardHeader>
            <CardTitle>Database Information</CardTitle>
            <CardDescription>View database details</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">ID</label>
                <p className="text-base">{database.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Vendor</label>
                <p className="text-base">{getVendorName(database.vendorId)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Database Code</label>
                <p className="text-base font-medium">{database.databaseCode}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Display Name</label>
                <p className="text-base">{database.displayName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Default Port</label>
                <p className="text-base">{database.defaultPort}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Status</label>
                <p className="text-base">
                  <Badge variant={database.active ? 'default' : 'secondary'}>{database.active ? 'Active' : 'Inactive'}</Badge>
                </p>
              </div>
              {database.description && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">Description</label>
                  <p className="text-base">{database.description}</p>
                </div>
              )}
              {database.defaultDriverClassName && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">Default Driver Class Name</label>
                  <p className="text-base font-mono text-sm">{database.defaultDriverClassName}</p>
                </div>
              )}
              {database.jdbcUrlTemplate && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">JDBC URL Template</label>
                  <p className="text-base font-mono text-sm">{database.jdbcUrlTemplate}</p>
                </div>
              )}
              {database.r2dbcUrlTemplate && (
                <div className="col-span-2">
                  <label className="text-sm font-medium text-muted-foreground">R2DBC URL Template</label>
                  <p className="text-base font-mono text-sm">{database.r2dbcUrlTemplate}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">Database not found</CardContent>
        </Card>
      )}
    </div>
  );
};

export default DatabaseManagementDetail;

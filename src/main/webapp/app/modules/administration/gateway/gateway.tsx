import React, { useEffect } from 'react';
import { Translate } from 'react-jhipster';
import { RefreshCw } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getGatewayRoutes } from '../administration.reducer';

export const GatewayPage = () => {
  const dispatch = useAppDispatch();
  const isFetching = useAppSelector(state => state.administration.loading);
  const routes = useAppSelector(state => state.administration.gateway.routes);

  useEffect(() => {
    dispatch(getGatewayRoutes());
  }, []);

  const metadata = instance => {
    const badges = [];
    Object.keys(instance).forEach((key, index) => {
      badges.push(
        <Badge key={`${key}-${index}`} variant="outline" className="text-xs font-normal">
          <span className="font-medium">{key}:</span> {String(instance[key])}
        </Badge>,
      );
    });
    return badges;
  };

  const badgeInfo = info => {
    if (info) {
      if (info.checks && info.checks.filter(check => check.status === 'PASSING').length === info.checks.length) {
        return <Badge variant="default">UP</Badge>;
      }
      return <Badge variant="destructive">DOWN</Badge>;
    }
    return <Badge variant="secondary">?</Badge>;
  };

  const instanceInfo = route => {
    if (route && route.serviceInstances && route.serviceInstances.length > 0) {
      return (
        <div className="space-y-2">
          {route.serviceInstances.map((instance, i) => (
            <div key={`${instance.instanceInfo}-info`} className="flex flex-col gap-2 py-1">
              <div className="flex items-center gap-2">
                <a
                  href={instance.uri}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm text-primary hover:underline font-medium"
                >
                  {instance.uri}
                </a>
                {badgeInfo(instance.healthService)}
              </div>
              {instance.metadata && Object.keys(instance.metadata).length > 0 && (
                <div className="flex flex-wrap gap-1.5">{metadata(instance.metadata)}</div>
              )}
            </div>
          ))}
        </div>
      );
    }
    return <span className="text-muted-foreground text-sm">No instances</span>;
  };

  const gatewayRoutes = () => {
    if (!isFetching) {
      dispatch(getGatewayRoutes());
    }
  };

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey="gateway.title">Gateway</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="gateway.subtitle">View and manage gateway routes and services</Translate>
          </p>
        </div>
        <Button onClick={gatewayRoutes} variant={isFetching ? 'destructive' : 'default'} disabled={isFetching}>
          <RefreshCw className={`mr-2 h-4 w-4 ${isFetching ? 'animate-spin' : ''}`} />
          <Translate component="span" contentKey="health.refresh.button">
            Refresh
          </Translate>
        </Button>
      </div>

      {/* Gateway Routes Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <Translate contentKey="gateway.routes.title">Gateway Routes</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="gateway.routes.description">List of all registered routes and their service instances</Translate>
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow key="header" className="border-b">
                  <TableHead>
                    <Translate contentKey="gateway.routes.url">URL</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="gateway.routes.service">Service</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="gateway.routes.servers">Available servers</Translate>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {routes && routes.length > 0 ? (
                  routes.map((route, i) => (
                    <TableRow key={`routes-${i}`} className="border-b hover:bg-muted/50">
                      <TableCell className="font-medium">{route.path}</TableCell>
                      <TableCell>{route.serviceId}</TableCell>
                      <TableCell>{instanceInfo(route)}</TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={3} className="h-24 text-center text-muted-foreground">
                      <Translate contentKey="gateway.routes.empty">No routes available</Translate>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default GatewayPage;

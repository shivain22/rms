import React, { useEffect } from 'react';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
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
    const spans = [];
    Object.keys(instance).map((key, index) => {
      spans.push(
        <span key={`${key.toString()}value`}>
          <Badge key={`${key.toString()}-containerbadge`} className="fw-normal">
            <Badge key={`${key.toString()}-badge`} variant="outline" className="fw-normal">
              {key}
            </Badge>
            {instance[key]}
          </Badge>
        </span>,
      );
    });
    return spans;
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
    if (route) {
      return (
        <div className="overflow-x-auto">
          <Table>
            <TableBody>
              {route.serviceInstances.map((instance, i) => (
                <TableRow key={`${instance.instanceInfo}-info`} className="border-b even:bg-muted/50">
                  <TableCell>
                    <a href={instance.uri} target="_blank" rel="noopener noreferrer">
                      {instance.uri}
                    </a>
                  </TableCell>
                  <TableCell>{badgeInfo(instance.healthService)}</TableCell>
                  <TableCell>{metadata(instance.metadata)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      );
    }
  };

  const gatewayRoutes = () => {
    if (!isFetching) {
      dispatch(getGatewayRoutes());
    }
  };

  return (
    <div>
      <h2>Gateway</h2>
      <p>
        <Button onClick={gatewayRoutes} variant={isFetching ? 'destructive' : 'default'} disabled={isFetching}>
          <FontAwesomeIcon icon="sync" className="mr-2" />
          <Translate component="span" contentKey="health.refresh.button">
            Refresh
          </Translate>
        </Button>
      </p>

      <div className="overflow-x-auto">
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
            {routes.map((route, i) => (
              <TableRow key={`routes-${i}`} className="border-b even:bg-muted/50">
                <TableCell>{route.path}</TableCell>
                <TableCell>{route.serviceId}</TableCell>
                <TableCell>{instanceInfo(route)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

export default GatewayPage;

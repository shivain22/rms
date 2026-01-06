import React, { useEffect, useState } from 'react';
import { Translate } from 'react-jhipster';
import { RefreshCw, Eye } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getSystemHealth } from '../administration.reducer';
import HealthModal from './health-modal';

export const HealthPage = () => {
  const [healthObject, setHealthObject] = useState({});
  const [showModal, setShowModal] = useState(false);
  const dispatch = useAppDispatch();

  const health = useAppSelector(state => state.administration.health);
  const isFetching = useAppSelector(state => state.administration.loading);

  useEffect(() => {
    dispatch(getSystemHealth());
  }, []);

  const fetchSystemHealth = () => {
    if (!isFetching) {
      dispatch(getSystemHealth());
    }
  };

  const getSystemHealthInfo = (name, healthObj) => () => {
    setShowModal(true);
    setHealthObject({ ...healthObj, name });
  };

  const getBadgeType = (status: string): 'destructive' | 'default' => {
    return status !== 'UP' ? 'destructive' : 'default';
  };

  const handleClose = () => setShowModal(false);

  const renderModal = () => <HealthModal healthObject={healthObject} handleClose={handleClose} showModal={showModal} />;

  const data = (health || {}).components || {};
  const healthData = Object.keys(data)
    .filter(key => key !== 'status')
    .map(key => ({
      name: key,
      status: data[key].status,
      details: data[key].details,
      healthObj: data[key],
    }));

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="health-page-heading" data-cy="healthPageHeading">
            <Translate contentKey="health.title">Health Checks</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="health.subtitle">Monitor system health and service status</Translate>
          </p>
        </div>
        <Button onClick={fetchSystemHealth} variant={isFetching ? 'destructive' : 'default'} disabled={isFetching}>
          <RefreshCw className={`mr-2 h-4 w-4 ${isFetching ? 'animate-spin' : ''}`} />
          <Translate component="span" contentKey="health.refresh.button">
            Refresh
          </Translate>
        </Button>
      </div>

      {/* Health Checks Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <Translate contentKey="health.table.title">System Health Status</Translate>
            </CardTitle>
            <CardDescription>
              <Translate contentKey="health.table.description">Overview of all system components and their current status</Translate>
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="rounded-md border">
            <Table aria-describedby="health-page-heading">
              <TableHeader>
                <TableRow className="border-b">
                  <TableHead>
                    <Translate contentKey="health.table.service">Service Name</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="health.table.status">Status</Translate>
                  </TableHead>
                  <TableHead>
                    <Translate contentKey="health.details.details">Details</Translate>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {healthData && healthData.length > 0 ? (
                  healthData.map((item, index) => (
                    <TableRow key={index} className="border-b hover:bg-muted/50">
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>
                        <Badge variant={getBadgeType(item.status)}>{item.status}</Badge>
                      </TableCell>
                      <TableCell>
                        {item.details ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0"
                            onClick={getSystemHealthInfo(item.name, item.healthObj)}
                          >
                            <span className="sr-only">View details</span>
                            <Eye className="h-4 w-4" />
                          </Button>
                        ) : (
                          <span className="text-muted-foreground text-sm">—</span>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={3} className="h-24 text-center text-muted-foreground">
                      <Translate contentKey="health.table.empty">No health data available</Translate>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
      {renderModal()}
    </div>
  );
};

export default HealthPage;

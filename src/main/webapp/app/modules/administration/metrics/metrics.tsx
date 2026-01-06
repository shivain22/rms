import React, { useEffect } from 'react';
import { RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  CacheMetrics,
  DatasourceMetrics,
  EndpointsRequestsMetrics,
  GarbageCollectorMetrics,
  HttpRequestMetrics,
  JvmMemory,
  JvmThreads,
  SystemMetrics,
  Translate,
} from 'react-jhipster';

import { APP_TIMESTAMP_FORMAT, APP_TWO_DIGITS_AFTER_POINT_NUMBER_FORMAT, APP_WHOLE_NUMBER_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getSystemMetrics, getSystemThreadDump } from '../administration.reducer';

export const MetricsPage = () => {
  const dispatch = useAppDispatch();
  const metrics = useAppSelector(state => state.administration.metrics);
  const isFetching = useAppSelector(state => state.administration.loading);
  const threadDump = useAppSelector(state => state.administration.threadDump);

  useEffect(() => {
    dispatch(getSystemMetrics());
    dispatch(getSystemThreadDump());
  }, [dispatch]);

  const getMetrics = () => {
    if (!isFetching) {
      dispatch(getSystemMetrics());
      dispatch(getSystemThreadDump());
    }
  };

  const hasMetrics = metrics && Object.keys(metrics).length > 0;

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey="metrics.title">Application Metrics</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="metrics.subtitle">Monitor application performance and system metrics</Translate>
          </p>
        </div>
        <Button onClick={getMetrics} variant={isFetching ? 'destructive' : 'default'} disabled={isFetching}>
          <RefreshCw className={`mr-2 h-4 w-4 ${isFetching ? 'animate-spin' : ''}`} />
          <Translate component="span" contentKey="health.refresh.button">
            Refresh
          </Translate>
        </Button>
      </div>

      {/* JVM Metrics Section */}
      {(metrics?.jvm || threadDump || metrics?.processMetrics) && (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Translate contentKey="metrics.jvm.title">JVM Metrics</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="metrics.jvm.description">Java Virtual Machine memory, threads, and system metrics</Translate>
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {metrics?.jvm ? (
                <div className="space-y-2">
                  <h4 className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="metrics.jvm.memory.title">Memory</Translate>
                  </h4>
                  <JvmMemory jvmMetrics={metrics.jvm} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} />
                </div>
              ) : null}
              {threadDump ? (
                <div className="space-y-2">
                  <h4 className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="metrics.jvm.threads.title">Threads</Translate>
                  </h4>
                  <JvmThreads jvmThreads={threadDump} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} />
                </div>
              ) : null}
              {metrics?.processMetrics ? (
                <div className="space-y-2">
                  <h4 className="text-sm font-medium text-muted-foreground">
                    <Translate contentKey="metrics.jvm.system.title">System</Translate>
                  </h4>
                  <SystemMetrics
                    systemMetrics={metrics.processMetrics}
                    wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT}
                    timestampFormat={APP_TIMESTAMP_FORMAT}
                  />
                </div>
              ) : null}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Garbage Collector Metrics */}
      {metrics?.garbageCollector && (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Translate contentKey="metrics.gc.title">Garbage Collector</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="metrics.gc.description">Garbage collection statistics and performance metrics</Translate>
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <GarbageCollectorMetrics garbageCollectorMetrics={metrics.garbageCollector} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} />
          </CardContent>
        </Card>
      )}

      {/* HTTP Request Metrics */}
      {metrics &&
      metrics['http.server.requests'] &&
      typeof metrics['http.server.requests'] === 'object' &&
      metrics['http.server.requests'].count !== undefined &&
      metrics['http.server.requests'].count !== null ? (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Translate contentKey="metrics.http.title">HTTP Requests</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="metrics.http.description">HTTP server request statistics and response times</Translate>
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <HttpRequestMetrics
              requestMetrics={metrics['http.server.requests']}
              twoDigitAfterPointFormat={APP_TWO_DIGITS_AFTER_POINT_NUMBER_FORMAT}
              wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT}
            />
          </CardContent>
        </Card>
      ) : null}

      {/* Endpoints Requests Metrics */}
      {metrics?.services && (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Translate contentKey="metrics.endpoints.title">Service Endpoints</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="metrics.endpoints.description">Request statistics for individual service endpoints</Translate>
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <EndpointsRequestsMetrics endpointsRequestsMetrics={metrics.services} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} />
          </CardContent>
        </Card>
      )}

      {/* Cache Metrics */}
      {metrics?.cache && (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Translate contentKey="metrics.cache.title">Cache</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="metrics.cache.description">Cache statistics and hit/miss ratios</Translate>
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <CacheMetrics cacheMetrics={metrics.cache} twoDigitAfterPointFormat={APP_TWO_DIGITS_AFTER_POINT_NUMBER_FORMAT} />
          </CardContent>
        </Card>
      )}

      {/* Database Metrics */}
      {metrics?.databases && JSON.stringify(metrics.databases) !== '{}' ? (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Translate contentKey="metrics.datasource.title">Data Sources</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="metrics.datasource.description">Database connection pool and datasource metrics</Translate>
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <DatasourceMetrics datasourceMetrics={metrics.databases} twoDigitAfterPointFormat={APP_TWO_DIGITS_AFTER_POINT_NUMBER_FORMAT} />
          </CardContent>
        </Card>
      ) : null}

      {/* Empty State */}
      {!hasMetrics && !isFetching && (
        <Card>
          <CardContent className="h-24 flex items-center justify-center text-muted-foreground">
            <Translate contentKey="metrics.empty">No metrics data available</Translate>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default MetricsPage;

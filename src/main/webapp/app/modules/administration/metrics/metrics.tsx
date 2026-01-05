import React, { useEffect } from 'react';
import { Button } from '@/components/ui/button';
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
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

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
  }, []);

  const getMetrics = () => {
    if (!isFetching) {
      dispatch(getSystemMetrics());
      dispatch(getSystemThreadDump());
    }
  };

  return (
    <div>
      <h2 id="metrics-page-heading" data-cy="metricsPageHeading">
        <Translate contentKey="metrics.title">Application Metrics</Translate>
      </h2>
      <p>
        <Button onClick={getMetrics} variant={isFetching ? 'destructive' : 'default'} disabled={isFetching}>
          <FontAwesomeIcon icon="sync" className="mr-2" />
          <Translate component="span" contentKey="health.refresh.button">
            Refresh
          </Translate>
        </Button>
      </p>
      <hr />

      <div className="grid grid-cols-12 gap-4">
        <div className="col-span-12">
          <h3>
            <Translate contentKey="metrics.jvm.title">JVM Metrics</Translate>
          </h3>
          <div className="grid grid-cols-12 gap-4 mt-4">
            <div className="col-span-12 md:col-span-4">
              {metrics?.jvm ? <JvmMemory jvmMetrics={metrics.jvm} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} /> : ''}
            </div>
            <div className="col-span-12 md:col-span-4">
              {threadDump ? <JvmThreads jvmThreads={threadDump} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} /> : ''}
            </div>
            <div className="col-span-12 md:col-span-4">
              {metrics?.processMetrics ? (
                <SystemMetrics
                  systemMetrics={metrics.processMetrics}
                  wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT}
                  timestampFormat={APP_TIMESTAMP_FORMAT}
                />
              ) : (
                ''
              )}
            </div>
          </div>
        </div>
      </div>

      {metrics?.garbageCollector ? (
        <GarbageCollectorMetrics garbageCollectorMetrics={metrics.garbageCollector} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} />
      ) : (
        ''
      )}
      {metrics && metrics['http.server.requests'] ? (
        <HttpRequestMetrics
          requestMetrics={metrics['http.server.requests']}
          twoDigitAfterPointFormat={APP_TWO_DIGITS_AFTER_POINT_NUMBER_FORMAT}
          wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT}
        />
      ) : (
        ''
      )}
      {metrics?.services ? (
        <EndpointsRequestsMetrics endpointsRequestsMetrics={metrics.services} wholeNumberFormat={APP_WHOLE_NUMBER_FORMAT} />
      ) : (
        ''
      )}

      {metrics?.cache ? (
        <div className="grid grid-cols-12 gap-4">
          <div className="col-span-12">
            <CacheMetrics cacheMetrics={metrics.cache} twoDigitAfterPointFormat={APP_TWO_DIGITS_AFTER_POINT_NUMBER_FORMAT} />
          </div>
        </div>
      ) : (
        ''
      )}

      {metrics?.databases && JSON.stringify(metrics.databases) !== '{}' ? (
        <div className="grid grid-cols-12 gap-4">
          <div className="col-span-12">
            <DatasourceMetrics datasourceMetrics={metrics.databases} twoDigitAfterPointFormat={APP_TWO_DIGITS_AFTER_POINT_NUMBER_FORMAT} />
          </div>
        </div>
      ) : (
        ''
      )}
    </div>
  );
};

export default MetricsPage;

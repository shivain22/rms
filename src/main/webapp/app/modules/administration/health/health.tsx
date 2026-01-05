import React, { useEffect, useState } from 'react';
import { Translate } from 'react-jhipster';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Row, Col } from '@/app/shared/layout/layout-utils';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

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

  return (
    <div>
      <h2 id="health-page-heading" data-cy="healthPageHeading">
        <Translate contentKey="health.title">Health Checks</Translate>
      </h2>
      <p>
        <Button onClick={fetchSystemHealth} variant={isFetching ? 'destructive' : 'default'} disabled={isFetching}>
          <FontAwesomeIcon icon="sync" />
          &nbsp;
          <Translate component="span" contentKey="health.refresh.button">
            Refresh
          </Translate>
        </Button>
      </p>
      <Row>
        <Col md="12">
          <Table aria-describedby="health-page-heading">
            <TableHeader>
              <TableRow>
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
              {Object.keys(data).map((configPropKey, configPropIndex) =>
                configPropKey !== 'status' ? (
                  <TableRow key={configPropIndex} className="even:bg-muted/50">
                    <TableCell>{configPropKey}</TableCell>
                    <TableCell>
                      <Badge variant={getBadgeType(data[configPropKey].status)}>{data[configPropKey].status}</Badge>
                    </TableCell>
                    <TableCell>
                      {data[configPropKey].details ? (
                        <a onClick={getSystemHealthInfo(configPropKey, data[configPropKey])} className="cursor-pointer">
                          <FontAwesomeIcon icon="eye" />
                        </a>
                      ) : null}
                    </TableCell>
                  </TableRow>
                ) : null,
              )}
            </TableBody>
          </Table>
        </Col>
      </Row>
      {renderModal()}
    </div>
  );
};

export default HealthPage;

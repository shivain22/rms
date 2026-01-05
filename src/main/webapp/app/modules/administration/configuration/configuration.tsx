import React, { useEffect, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Translate } from 'react-jhipster';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getConfigurations, getEnv } from '../administration.reducer';

export const ConfigurationPage = () => {
  const [filter, setFilter] = useState('');
  const [reversePrefix, setReversePrefix] = useState(false);
  const [reverseProperties, setReverseProperties] = useState(false);
  const dispatch = useAppDispatch();

  const configuration = useAppSelector(state => state.administration.configuration);

  useEffect(() => {
    dispatch(getConfigurations());
    dispatch(getEnv());
  }, []);

  const changeFilter = evt => setFilter(evt.target.value);

  const envFilterFn = configProp => configProp.toUpperCase().includes(filter.toUpperCase());

  const propsFilterFn = configProp => configProp.prefix.toUpperCase().includes(filter.toUpperCase());

  const changeReversePrefix = () => setReversePrefix(!reversePrefix);

  const changeReverseProperties = () => setReverseProperties(!reverseProperties);

  const getContextList = contexts =>
    Object.values(contexts)
      .map((v: any) => v.beans)
      .reduce((acc, e) => ({ ...acc, ...e }));

  const configProps = configuration?.configProps ?? {};

  const env = configuration?.env ?? {};

  return (
    <div>
      <h2 id="configuration-page-heading" data-cy="configurationPageHeading">
        <Translate contentKey="configuration.title">Configuration</Translate>
      </h2>
      <span>
        <Translate contentKey="configuration.filter">Filter</Translate>
      </span>{' '}
      <Input
        type="search"
        value={filter}
        onChange={changeFilter}
        name="search"
        id="search"
        placeholder="Filter configuration..."
        className="max-w-sm"
      />
      <label>Spring configuration</label>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="border-b">
              <TableHead onClick={changeReversePrefix}>
                <Translate contentKey="configuration.table.prefix">Prefix</Translate>
              </TableHead>
              <TableHead onClick={changeReverseProperties}>
                <Translate contentKey="configuration.table.properties">Properties</Translate>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {configProps.contexts
              ? Object.values(getContextList(configProps.contexts))
                  .filter(propsFilterFn)
                  .map((property: any, propIndex) => (
                    <TableRow key={propIndex} className="border-b even:bg-muted/50">
                      <TableCell>{property.prefix}</TableCell>
                      <TableCell>
                        {Object.keys(property.properties).map((propKey, index) => (
                          <div key={index} className="grid grid-cols-12 gap-2 mb-2">
                            <div className="col-span-12 md:col-span-4">{propKey}</div>
                            <div className="col-span-12 md:col-span-8">
                              <Badge variant="secondary" className="float-end break">
                                {JSON.stringify(property.properties[propKey])}
                              </Badge>
                            </div>
                          </div>
                        ))}
                      </TableCell>
                    </TableRow>
                  ))
              : null}
          </TableBody>
        </Table>
      </div>
      {env.propertySources
        ? env.propertySources.map((envKey, envIndex) => (
            <div key={envIndex}>
              <h4>
                <span>{envKey.name}</span>
              </h4>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow key={envIndex} className="border-b">
                      <TableHead className="w-40">Property</TableHead>
                      <TableHead className="w-60">Value</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {Object.keys(envKey.properties)
                      .filter(envFilterFn)
                      .map((propKey, propIndex) => (
                        <TableRow key={propIndex} className="border-b even:bg-muted/50">
                          <TableCell className="break">{propKey}</TableCell>
                          <TableCell className="break">
                            <Badge variant="secondary" className="float-end break">
                              {envKey.properties[propKey].value}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          ))
        : null}
    </div>
  );
};

export default ConfigurationPage;

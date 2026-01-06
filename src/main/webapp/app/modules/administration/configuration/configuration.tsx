import React, { useEffect, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Translate } from 'react-jhipster';
import { ArrowUpDown, Search } from 'lucide-react';

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

  const springConfigData = configProps.contexts ? Object.values(getContextList(configProps.contexts)).filter(propsFilterFn) : [];

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" id="configuration-page-heading" data-cy="configurationPageHeading">
            <Translate contentKey="configuration.title">Configuration</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="configuration.subtitle">View and manage application configuration properties</Translate>
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="search"
              value={filter}
              onChange={changeFilter}
              name="search"
              id="search"
              placeholder="Filter configuration..."
              className="pl-9 max-w-sm"
            />
          </div>
        </div>
      </div>

      {/* Spring Configuration Table */}
      {springConfigData.length > 0 && (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Translate contentKey="configuration.spring.title">Spring Configuration</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="configuration.spring.description">Configuration properties organized by prefix</Translate>
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent className="p-0">
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow className="border-b">
                    <TableHead className="cursor-pointer hover:bg-muted/50" onClick={changeReversePrefix}>
                      <div className="flex items-center">
                        <Translate contentKey="configuration.table.prefix">Prefix</Translate>
                        <ArrowUpDown className="h-3 w-3 ml-1" />
                      </div>
                    </TableHead>
                    <TableHead className="cursor-pointer hover:bg-muted/50" onClick={changeReverseProperties}>
                      <div className="flex items-center">
                        <Translate contentKey="configuration.table.properties">Properties</Translate>
                        <ArrowUpDown className="h-3 w-3 ml-1" />
                      </div>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {springConfigData.map((property: any, propIndex) => (
                    <TableRow key={propIndex} className="border-b hover:bg-muted/50">
                      <TableCell className="font-medium">{property.prefix}</TableCell>
                      <TableCell>
                        <div className="space-y-2">
                          {Object.keys(property.properties).map((propKey, index) => (
                            <div key={index} className="flex items-start gap-2">
                              <span className="text-sm font-medium text-muted-foreground min-w-[120px]">{propKey}:</span>
                              <Badge variant="secondary" className="text-xs font-normal break-all">
                                {JSON.stringify(property.properties[propKey])}
                              </Badge>
                            </div>
                          ))}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Environment Properties */}
      {env.propertySources &&
        env.propertySources.map((envKey, envIndex) => {
          const filteredProperties = Object.keys(envKey.properties).filter(envFilterFn);
          if (filteredProperties.length === 0) return null;

          return (
            <Card key={envIndex}>
              <CardHeader>
                <div>
                  <CardTitle>{envKey.name}</CardTitle>
                  <CardDescription>
                    <Translate contentKey="configuration.env.description">Environment-specific configuration properties</Translate>
                  </CardDescription>
                </div>
              </CardHeader>
              <CardContent className="p-0">
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow className="border-b">
                        <TableHead className="w-1/3">
                          <Translate contentKey="configuration.table.property">Property</Translate>
                        </TableHead>
                        <TableHead className="w-2/3">
                          <Translate contentKey="configuration.table.value">Value</Translate>
                        </TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {filteredProperties.map((propKey, propIndex) => (
                        <TableRow key={propIndex} className="border-b hover:bg-muted/50">
                          <TableCell className="font-medium break-all">{propKey}</TableCell>
                          <TableCell>
                            <Badge variant="secondary" className="text-xs font-normal break-all">
                              {envKey.properties[propKey].value}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </CardContent>
            </Card>
          );
        })}

      {/* Empty State */}
      {springConfigData.length === 0 && (!env.propertySources || env.propertySources.length === 0) && (
        <Card>
          <CardContent className="py-12">
            <div className="text-center text-muted-foreground">
              <Translate contentKey="configuration.empty">No configuration data available</Translate>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default ConfigurationPage;

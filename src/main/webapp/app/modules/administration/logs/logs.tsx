import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Translate } from 'react-jhipster';
import {
  useReactTable,
  getCoreRowModel,
  getPaginationRowModel,
  getFilteredRowModel,
  ColumnDef,
  flexRender,
  ColumnFiltersState,
} from '@tanstack/react-table';
import { Search, RefreshCw } from 'lucide-react';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { changeLogLevel, getLoggers } from '../administration.reducer';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

type LoggerRow = {
  name: string;
  level: string;
};

export const LogsPage = () => {
  const [filter, setFilter] = useState('');
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const logs = useAppSelector(state => state.administration.logs);
  const isFetching = useAppSelector(state => state.administration.loading);
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getLoggers());
  }, [dispatch]);

  const handleChangeLevel = useCallback(
    (loggerName: string, level: string) => {
      dispatch(changeLogLevel(loggerName, level));
    },
    [dispatch],
  );

  const changeFilter = (evt: React.ChangeEvent<HTMLInputElement>) => {
    setFilter(evt.target.value);
    setColumnFilters([{ id: 'name', value: evt.target.value }]);
  };

  const loggers: LoggerRow[] = logs && Array.isArray(logs.loggers) ? logs.loggers : [];

  // Table columns definition
  const columns = useMemo<ColumnDef<LoggerRow>[]>(
    () => [
      {
        accessorKey: 'name',
        header: () => <Translate contentKey="logs.table.name">Name</Translate>,
        cell: ({ row }) => <div className="font-medium">{row.getValue('name')}</div>,
      },
      {
        id: 'level',
        header: () => <Translate contentKey="logs.table.level">Level</Translate>,
        cell({ row }) {
          const logger = row.original;
          const currentLevel = logger.level;
          const levels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'];

          const getButtonClass = (level: string, current: string) => {
            const isActive = level === current;
            const baseClass = 'h-7 px-2 text-xs font-medium transition-colors border';
            if (isActive) {
              switch (level) {
                case 'TRACE':
                  return `${baseClass} bg-blue-500 hover:bg-blue-600 text-white border-blue-600`;
                case 'DEBUG':
                  return `${baseClass} bg-green-500 hover:bg-green-600 text-white border-green-600`;
                case 'INFO':
                  return `${baseClass} bg-cyan-500 hover:bg-cyan-600 text-white border-cyan-600`;
                case 'WARN':
                  return `${baseClass} bg-yellow-500 hover:bg-yellow-600 text-white border-yellow-600`;
                case 'ERROR':
                  return `${baseClass} bg-red-500 hover:bg-red-600 text-white border-red-600`;
                case 'OFF':
                  return `${baseClass} bg-gray-500 hover:bg-gray-600 text-white border-gray-600`;
                default:
                  return baseClass;
              }
            }
            return `${baseClass} bg-background hover:bg-muted border-border`;
          };

          return (
            <div className="flex items-center gap-1 flex-wrap">
              {levels.map(level => (
                <Button
                  key={level}
                  variant="ghost"
                  size="sm"
                  disabled={isFetching}
                  onClick={() => handleChangeLevel(logger.name, level)}
                  className={getButtonClass(level, currentLevel)}
                >
                  {level}
                </Button>
              ))}
            </div>
          );
        },
      },
    ],
    [isFetching, handleChangeLevel],
  );

  const table = useReactTable({
    data: loggers,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    onColumnFiltersChange: setColumnFilters,
    getFilteredRowModel: getFilteredRowModel(),
    state: {
      columnFilters,
    },
    initialState: {
      pagination: {
        pageSize: 25,
      },
    },
  });

  return (
    <div className="space-y-8 w-full">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            <Translate contentKey="logs.title">Logs</Translate>
          </h1>
          <p className="text-muted-foreground mt-1.5">
            <Translate contentKey="logs.subtitle">Manage logger levels and monitor application logging</Translate>
          </p>
        </div>
        <Button onClick={() => dispatch(getLoggers())} variant={isFetching ? 'destructive' : 'default'} disabled={isFetching}>
          <RefreshCw className={`mr-2 h-4 w-4 ${isFetching ? 'animate-spin' : ''}`} />
          <Translate component="span" contentKey="health.refresh.button">
            Refresh
          </Translate>
        </Button>
      </div>

      {/* Logger Summary */}
      <div className="text-sm text-muted-foreground">
        <Translate contentKey="logs.nbloggers" interpolate={{ total: loggers.length }}>
          There are {loggers.length.toString()} loggers.
        </Translate>
      </div>

      {/* Loggers Table */}
      <Card>
        <CardHeader>
          <div className="flex flex-col space-y-4 sm:flex-row sm:items-center sm:justify-between sm:space-y-0">
            <div>
              <CardTitle>
                <Translate contentKey="logs.table.title">Logger Configuration</Translate>
              </CardTitle>
              <CardDescription>
                <Translate contentKey="logs.table.description">Configure logging levels for each logger in the application</Translate>
              </CardDescription>
            </div>
            <div className="flex items-center space-x-2">
              <Search className="h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                value={filter}
                onChange={changeFilter}
                name="search"
                id="search"
                placeholder="Filter loggers..."
                className="max-w-sm"
                disabled={isFetching}
              />
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="rounded-md border">
            <Table aria-describedby="logs-page-heading">
              <TableHeader>
                {table.getHeaderGroups().map(headerGroup => (
                  <TableRow key={headerGroup.id} className="border-b">
                    {headerGroup.headers.map(header => (
                      <TableHead key={header.id}>
                        {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                      </TableHead>
                    ))}
                  </TableRow>
                ))}
              </TableHeader>
              <TableBody>
                {table.getRowModel().rows?.length ? (
                  table.getRowModel().rows.map(row => (
                    <TableRow key={row.id} className="border-b hover:bg-muted/50">
                      {row.getVisibleCells().map(cell => (
                        <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
                      ))}
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={columns.length} className="h-24 text-center text-muted-foreground">
                      <Translate contentKey="logs.table.empty">No loggers found</Translate>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
          {/* Pagination */}
          <div className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="text-sm text-muted-foreground">
              Showing {table.getState().pagination.pageIndex * table.getState().pagination.pageSize + 1} to{' '}
              {Math.min(
                (table.getState().pagination.pageIndex + 1) * table.getState().pagination.pageSize,
                table.getFilteredRowModel().rows.length,
              )}{' '}
              of {table.getFilteredRowModel().rows.length} logger(s)
            </div>
            <div className="flex items-center gap-2">
              <Button variant="outline" className="h-8 w-8 p-0" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>
                <span className="sr-only">Go to previous page</span>
                {'<'}
              </Button>
              <div className="flex items-center gap-2 text-sm font-medium">
                Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
              </div>
              <Button variant="outline" className="h-8 w-8 p-0" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>
                <span className="sr-only">Go to next page</span>
                {'>'}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default LogsPage;

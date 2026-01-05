import './home.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Translate } from 'react-jhipster';
import {
  useReactTable,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  ColumnDef,
  flexRender,
  SortingState,
  ColumnFiltersState,
} from '@tanstack/react-table';
import {
  ArrowUp,
  ArrowDown,
  TrendingUp,
  Users,
  DollarSign,
  Activity,
  Plus,
  MoreHorizontal,
  GripVertical,
  CheckCircle2,
  Circle,
} from 'lucide-react';

import { REDIRECT_URL, getLoginUrl } from 'app/shared/util/url-utils';
import { useAppSelector } from 'app/config/store';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

// Mock data types
type MetricCard = {
  title: string;
  value: string;
  change: string;
  changeType: 'positive' | 'negative';
  description: string;
  icon: React.ReactNode;
};

type DocumentRow = {
  id: string;
  header: string;
  sectionType: string;
  status: 'done' | 'in-process';
  target: number;
  limit: number;
  reviewer: string;
};

// Mock data
const mockMetrics: MetricCard[] = [
  {
    title: 'Total Revenue',
    value: '$1,250.00',
    change: '+12.5%',
    changeType: 'positive',
    description: 'Trending up this month',
    icon: <DollarSign className="h-4 w-4" />,
  },
  {
    title: 'New Customers',
    value: '1,234',
    change: '-20%',
    changeType: 'negative',
    description: 'Down 20% this period',
    icon: <Users className="h-4 w-4" />,
  },
  {
    title: 'Active Accounts',
    value: '45,678',
    change: '+12.5%',
    changeType: 'positive',
    description: 'Strong user retention',
    icon: <Activity className="h-4 w-4" />,
  },
  {
    title: 'Growth Rate',
    value: '4.5%',
    change: '+4.5%',
    changeType: 'positive',
    description: 'Steady performance increase',
    icon: <TrendingUp className="h-4 w-4" />,
  },
];

const mockDocuments: DocumentRow[] = [
  {
    id: '1',
    header: 'Cover page',
    sectionType: 'Cover page',
    status: 'in-process',
    target: 18,
    limit: 5,
    reviewer: 'Eddie Lake',
  },
  {
    id: '2',
    header: 'Table of contents',
    sectionType: 'Table of contents',
    status: 'done',
    target: 29,
    limit: 24,
    reviewer: 'Eddie Lake',
  },
  {
    id: '3',
    header: 'Executive summary',
    sectionType: 'Narrative',
    status: 'done',
    target: 10,
    limit: 13,
    reviewer: 'Eddie Lake',
  },
  {
    id: '4',
    header: 'Technical approach',
    sectionType: 'Narrative',
    status: 'done',
    target: 27,
    limit: 23,
    reviewer: 'Jamik Tashpulatov',
  },
  {
    id: '5',
    header: 'Design',
    sectionType: 'Narrative',
    status: 'in-process',
    target: 2,
    limit: 16,
    reviewer: 'Jamik Tashpulatov',
  },
  {
    id: '6',
    header: 'Capabilities',
    sectionType: 'Narrative',
    status: 'in-process',
    target: 20,
    limit: 8,
    reviewer: 'Jamik Tashpulatov',
  },
  {
    id: '7',
    header: 'Integration with existing systems',
    sectionType: 'Narrative',
    status: 'in-process',
    target: 19,
    limit: 21,
    reviewer: 'Jamik Tashpulatov',
  },
  {
    id: '8',
    header: 'Innovation and Advantages',
    sectionType: 'Narrative',
    status: 'done',
    target: 25,
    limit: 26,
    reviewer: 'Assign reviewer',
  },
  {
    id: '9',
    header: "Overview of EMR's Innovative Solutions",
    sectionType: 'Technical content',
    status: 'done',
    target: 7,
    limit: 23,
    reviewer: 'Assign reviewer',
  },
  {
    id: '10',
    header: 'Advanced Algorithms and Machine Learning',
    sectionType: 'Narrative',
    status: 'done',
    target: 30,
    limit: 28,
    reviewer: 'Assign reviewer',
  },
];

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);
  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [rowSelection, setRowSelection] = useState({});

  useEffect(() => {
    const redirectURL = localStorage.getItem(REDIRECT_URL);
    if (redirectURL) {
      localStorage.removeItem(REDIRECT_URL);
      location.href = `${location.origin}${redirectURL}`;
    }
  });

  // Table columns definition
  const columns = useMemo<ColumnDef<DocumentRow>[]>(
    () => [
      {
        id: 'select',
        header: ({ table }) => (
          <div className="flex items-center space-x-2">
            <GripVertical className="h-4 w-4 text-muted-foreground" />
            <input
              type="checkbox"
              checked={table.getIsAllPageRowsSelected()}
              onChange={e => table.toggleAllPageRowsSelected(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300"
            />
          </div>
        ),
        cell: ({ row }) => (
          <div className="flex items-center space-x-2">
            <GripVertical className="h-4 w-4 text-muted-foreground cursor-move" />
            <input
              type="checkbox"
              checked={row.getIsSelected()}
              onChange={e => row.toggleSelected(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300"
            />
          </div>
        ),
        enableSorting: false,
        enableHiding: false,
      },
      {
        accessorKey: 'header',
        header: 'Header',
        cell: ({ row }) => <div className="font-medium">{row.getValue('header')}</div>,
      },
      {
        accessorKey: 'sectionType',
        header: 'Section Type',
        cell: ({ row }) => <div className="text-muted-foreground">{row.getValue('sectionType')}</div>,
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: ({ row }) => {
          const status = row.getValue('status') as string;
          return (
            <div className="flex items-center space-x-2">
              {status === 'done' ? <CheckCircle2 className="h-4 w-4 text-green-500" /> : <Circle className="h-4 w-4 text-gray-400" />}
              <Badge variant={status === 'done' ? 'default' : 'outline'}>{status === 'done' ? 'Done' : 'In Process'}</Badge>
            </div>
          );
        },
      },
      {
        accessorKey: 'target',
        header: 'Target',
        cell: ({ row }) => <div>{row.getValue('target')}</div>,
      },
      {
        accessorKey: 'limit',
        header: 'Limit',
        cell: ({ row }) => <div>{row.getValue('limit')}</div>,
      },
      {
        accessorKey: 'reviewer',
        header: 'Reviewer',
        cell: ({ row }) => {
          const reviewer = row.getValue('reviewer') as string;
          return reviewer === 'Assign reviewer' ? (
            <Button variant="outline" size="sm">
              Assign reviewer
            </Button>
          ) : (
            <div>{reviewer}</div>
          );
        },
      },
      {
        id: 'actions',
        cell: ({ row }) => (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="h-8 w-8 p-0">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem>Edit</DropdownMenuItem>
              <DropdownMenuItem>Delete</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ),
      },
    ],
    [],
  );

  const table = useReactTable({
    data: mockDocuments,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    onSortingChange: setSorting,
    getSortedRowModel: getSortedRowModel(),
    onColumnFiltersChange: setColumnFilters,
    getFilteredRowModel: getFilteredRowModel(),
    onRowSelectionChange: setRowSelection,
    state: {
      sorting,
      columnFilters,
      rowSelection,
    },
    initialState: {
      pagination: {
        pageSize: 10,
      },
    },
  });

  // If user is not logged in, show login page
  if (!account?.login) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen px-4">
        <div className="flex flex-col items-center justify-center gap-8">
          <div className="flex items-center justify-center">
            <img src="content/images/atpar_logo.jpg" alt="aPAR Logo" className="max-w-md w-full h-auto" />
          </div>
          <Button
            size="lg"
            variant="black"
            className="min-w-[200px] shadow-sm hover:shadow-md transition-shadow"
            onClick={() => navigate('/sign-in', { state: { from: pageLocation } })}
          >
            Enter
          </Button>
        </div>
      </div>
    );
  }

  // Dashboard view for logged-in users
  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Documents</h1>
          <p className="text-muted-foreground">Manage your documents and sections</p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          Quick Create
        </Button>
      </div>

      {/* Metric Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {mockMetrics.map((metric, index) => (
          <Card key={index}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{metric.title}</CardTitle>
              <div className="text-muted-foreground">{metric.icon}</div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{metric.value}</div>
              <div className="flex items-center space-x-2 text-xs text-muted-foreground mt-1">
                {metric.changeType === 'positive' ? (
                  <ArrowUp className="h-3 w-3 text-green-500" />
                ) : (
                  <ArrowDown className="h-3 w-3 text-red-500" />
                )}
                <span className={metric.changeType === 'positive' ? 'text-green-500' : 'text-red-500'}>{metric.change}</span>
                <span className="text-muted-foreground">{metric.description}</span>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Chart Section Placeholder */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Total Visitors</CardTitle>
              <CardDescription>Total for the last 3 months</CardDescription>
            </div>
            <div className="flex space-x-2">
              <Button variant="outline" size="sm">
                Last 3 months
              </Button>
              <Button variant="ghost" size="sm">
                Last 30 days
              </Button>
              <Button variant="ghost" size="sm">
                Last 7 days
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] flex items-center justify-center text-muted-foreground">
            Chart visualization placeholder - Integrate with your preferred charting library (e.g., Recharts, Chart.js)
          </div>
        </CardContent>
      </Card>

      {/* Documents Table Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Document Sections</CardTitle>
              <CardDescription>Manage document sections and their status</CardDescription>
            </div>
            <div className="flex items-center space-x-2">
              <Button variant="outline" size="sm">
                Customize Columns
              </Button>
              <Button size="sm">
                <Plus className="mr-2 h-4 w-4" />
                Add Section
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="outline" className="w-full">
            <TabsList>
              <TabsTrigger value="outline">Outline</TabsTrigger>
              <TabsTrigger value="past-performance">
                Past Performance{' '}
                <Badge variant="secondary" className="ml-2">
                  3
                </Badge>
              </TabsTrigger>
              <TabsTrigger value="key-personnel">
                Key Personnel{' '}
                <Badge variant="secondary" className="ml-2">
                  2
                </Badge>
              </TabsTrigger>
              <TabsTrigger value="focus-documents">Focus Documents</TabsTrigger>
            </TabsList>
            <TabsContent value="outline" className="space-y-4">
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    {table.getHeaderGroups().map(headerGroup => (
                      <TableRow key={headerGroup.id}>
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
                        <TableRow key={row.id} data-state={row.getIsSelected() && 'selected'}>
                          {row.getVisibleCells().map(cell => (
                            <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
                          ))}
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={columns.length} className="h-24 text-center">
                          No results.
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>
              <div className="flex items-center justify-between px-2">
                <div className="flex-1 text-sm text-muted-foreground">
                  {table.getFilteredSelectedRowModel().rows.length} of {table.getFilteredRowModel().rows.length} row(s) selected.
                </div>
                <div className="flex items-center space-x-6 lg:space-x-8">
                  <div className="flex items-center space-x-2">
                    <p className="text-sm font-medium">Rows per page</p>
                    <Select
                      value={`${table.getState().pagination.pageSize}`}
                      onValueChange={value => {
                        table.setPageSize(Number(value));
                      }}
                    >
                      <SelectTrigger className="h-8 w-[70px]">
                        <SelectValue placeholder={table.getState().pagination.pageSize} />
                      </SelectTrigger>
                      <SelectContent side="top">
                        {[10, 20, 30, 40, 50].map(pageSize => (
                          <SelectItem key={pageSize} value={`${pageSize}`}>
                            {pageSize}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex w-[100px] items-center justify-center text-sm font-medium">
                    Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
                  </div>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="outline"
                      className="hidden h-8 w-8 p-0 lg:flex"
                      onClick={() => table.setPageIndex(0)}
                      disabled={!table.getCanPreviousPage()}
                    >
                      <span className="sr-only">Go to first page</span>
                      {'<<'}
                    </Button>
                    <Button
                      variant="outline"
                      className="h-8 w-8 p-0"
                      onClick={() => table.previousPage()}
                      disabled={!table.getCanPreviousPage()}
                    >
                      <span className="sr-only">Go to previous page</span>
                      {'<'}
                    </Button>
                    <Button variant="outline" className="h-8 w-8 p-0" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>
                      <span className="sr-only">Go to next page</span>
                      {'>'}
                    </Button>
                    <Button
                      variant="outline"
                      className="hidden h-8 w-8 p-0 lg:flex"
                      onClick={() => table.setPageIndex(table.getPageCount() - 1)}
                      disabled={!table.getCanNextPage()}
                    >
                      <span className="sr-only">Go to last page</span>
                      {'>>'}
                    </Button>
                  </div>
                </div>
              </div>
            </TabsContent>
            <TabsContent value="past-performance">Past Performance content goes here</TabsContent>
            <TabsContent value="key-personnel">Key Personnel content goes here</TabsContent>
            <TabsContent value="focus-documents">Focus Documents content goes here</TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
};

export default Home;

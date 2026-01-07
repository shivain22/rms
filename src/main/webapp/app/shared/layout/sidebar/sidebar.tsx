import React, { Suspense, useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';
import {
  LayoutDashboard,
  Building2,
  Server,
  Activity,
  Heart,
  Settings,
  FileText,
  BookOpen,
  Home,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { Translate } from 'react-jhipster';
import { Button } from '@/components/ui/button';

const EntitiesMenuItems = React.lazy(() => import('app/entities/menu').catch(() => import('app/shared/error/error-loading')));

interface SidebarProps {
  isAuthenticated: boolean;
  isAdmin: boolean;
  isOpenAPIEnabled: boolean;
}

const SIDEBAR_STORAGE_KEY = 'sidebar-collapsed';

const Sidebar: React.FC<SidebarProps> = ({ isAuthenticated, isAdmin, isOpenAPIEnabled }) => {
  const location = useLocation();
  const [isCollapsed, setIsCollapsed] = useState(() => {
    const stored = localStorage.getItem(SIDEBAR_STORAGE_KEY);
    return stored ? JSON.parse(stored) : false;
  });

  useEffect(() => {
    localStorage.setItem(SIDEBAR_STORAGE_KEY, JSON.stringify(isCollapsed));
  }, [isCollapsed]);

  const isActive = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/');

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  const sidebarWidth = isCollapsed ? 'w-16' : 'w-64';
  const logoTextHidden = isCollapsed ? 'hidden' : 'flex';

  return (
    <div className="hidden md:flex md:flex-shrink-0 md:h-screen">
      <div
        className={cn(
          'flex flex-col h-full border-r border-border/20 bg-slate-50 dark:bg-slate-900/50 overflow-hidden transition-all duration-300',
          sidebarWidth,
        )}
      >
        {/* Logo Section */}
        <div className="flex items-center justify-between h-16 px-4 border-b border-border/20 flex-shrink-0">
          <Link to="/" className="flex items-center space-x-2 min-w-0">
            <div className="w-8 h-8 bg-gradient-to-br from-orange-500 to-green-500 rounded-lg flex items-center justify-center flex-shrink-0">
              <span className="text-white font-bold text-lg">a</span>
            </div>
            <div className={cn('flex-col', logoTextHidden)}>
              <span className="text-lg font-bold text-foreground">PAR</span>
              <span className="text-xs text-muted-foreground">intelligence</span>
            </div>
          </Link>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 flex-shrink-0"
            onClick={toggleSidebar}
            aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {isCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </Button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-2 py-4 space-y-1 overflow-y-auto overflow-x-hidden">
          {/* Home */}
          <Link
            to="/"
            className={cn(
              'flex items-center rounded-md text-sm font-medium transition-colors',
              isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
              isActive('/') ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
            )}
            title={isCollapsed ? 'Home' : undefined}
          >
            <Home className="h-4 w-4 flex-shrink-0" />
            {!isCollapsed && (
              <span>
                <Translate contentKey="global.menu.home">Home</Translate>
              </span>
            )}
          </Link>

          {/* Entities Menu Items - Only show if there are actual entities */}
          {isAuthenticated && (
            <Suspense fallback={null}>
              <EntitiesMenuItems isCollapsed={isCollapsed} />
            </Suspense>
          )}

          {/* Administration Menu Items */}
          {isAuthenticated && isAdmin && (
            <>
              <Link
                to="/admin/gateway"
                className={cn(
                  'flex items-center rounded-md text-sm font-medium transition-colors',
                  isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                  isActive('/admin/gateway')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
                title={isCollapsed ? 'Gateway' : undefined}
              >
                <LayoutDashboard className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && (
                  <span>
                    <Translate contentKey="global.menu.admin.gateway">Gateway</Translate>
                  </span>
                )}
              </Link>

              <Link
                to="/admin/tenant-management"
                className={cn(
                  'flex items-center rounded-md text-sm font-medium transition-colors',
                  isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                  isActive('/admin/tenant-management')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
                title={isCollapsed ? 'Tenant Management' : undefined}
              >
                <Building2 className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && (
                  <span>
                    <Translate contentKey="global.menu.admin.tenantManagement">Tenant Management</Translate>
                  </span>
                )}
              </Link>

              <Link
                to="/admin/platform-management"
                className={cn(
                  'flex items-center rounded-md text-sm font-medium transition-colors',
                  isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                  isActive('/admin/platform-management')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
                title={isCollapsed ? 'Platform Management' : undefined}
              >
                <Server className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && (
                  <span>
                    <Translate contentKey="global.menu.admin.platformManagement">Platform Management</Translate>
                  </span>
                )}
              </Link>

              <Link
                to="/admin/metrics"
                className={cn(
                  'flex items-center rounded-md text-sm font-medium transition-colors',
                  isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                  isActive('/admin/metrics')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
                title={isCollapsed ? 'Metrics' : undefined}
              >
                <Activity className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && (
                  <span>
                    <Translate contentKey="global.menu.admin.metrics">Metrics</Translate>
                  </span>
                )}
              </Link>

              <Link
                to="/admin/health"
                className={cn(
                  'flex items-center rounded-md text-sm font-medium transition-colors',
                  isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                  isActive('/admin/health')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
                title={isCollapsed ? 'Health' : undefined}
              >
                <Heart className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && (
                  <span>
                    <Translate contentKey="global.menu.admin.health">Health</Translate>
                  </span>
                )}
              </Link>

              <Link
                to="/admin/configuration"
                className={cn(
                  'flex items-center rounded-md text-sm font-medium transition-colors',
                  isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                  isActive('/admin/configuration')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
                title={isCollapsed ? 'Configuration' : undefined}
              >
                <Settings className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && (
                  <span>
                    <Translate contentKey="global.menu.admin.configuration">Configuration</Translate>
                  </span>
                )}
              </Link>

              <Link
                to="/admin/logs"
                className={cn(
                  'flex items-center rounded-md text-sm font-medium transition-colors',
                  isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                  isActive('/admin/logs')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
                title={isCollapsed ? 'Logs' : undefined}
              >
                <FileText className="h-4 w-4 flex-shrink-0" />
                {!isCollapsed && (
                  <span>
                    <Translate contentKey="global.menu.admin.logs">Logs</Translate>
                  </span>
                )}
              </Link>

              {isOpenAPIEnabled && (
                <Link
                  to="/admin/docs"
                  className={cn(
                    'flex items-center rounded-md text-sm font-medium transition-colors',
                    isCollapsed ? 'justify-center px-2 py-2' : 'space-x-3 px-3 py-2',
                    isActive('/admin/docs')
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                  )}
                  title={isCollapsed ? 'API' : undefined}
                >
                  <BookOpen className="h-4 w-4 flex-shrink-0" />
                  {!isCollapsed && (
                    <span>
                      <Translate contentKey="global.menu.admin.apidocs">API</Translate>
                    </span>
                  )}
                </Link>
              )}
            </>
          )}
        </nav>
      </div>
    </div>
  );
};

export default Sidebar;

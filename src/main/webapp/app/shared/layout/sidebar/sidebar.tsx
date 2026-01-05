import React, { Suspense } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { LayoutDashboard, Building2, Activity, Heart, Settings, FileText, BookOpen, Home } from 'lucide-react';
import { Translate } from 'react-jhipster';

const EntitiesMenuItems = React.lazy(() => import('app/entities/menu').catch(() => import('app/shared/error/error-loading')));

interface SidebarProps {
  isAuthenticated: boolean;
  isAdmin: boolean;
  isOpenAPIEnabled: boolean;
}

const Sidebar: React.FC<SidebarProps> = ({ isAuthenticated, isAdmin, isOpenAPIEnabled }) => {
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/');

  return (
    <div className="hidden md:flex md:flex-shrink-0">
      <div className="flex flex-col w-64 border-r bg-background">
        {/* Logo Section */}
        <div className="flex items-center h-16 px-6 border-b">
          <Link to="/" className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gradient-to-br from-orange-500 to-green-500 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">a</span>
            </div>
            <div className="flex flex-col">
              <span className="text-lg font-bold text-foreground">PAR</span>
              <span className="text-xs text-muted-foreground">intelligence</span>
            </div>
          </Link>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
          {/* Home */}
          <Link
            to="/"
            className={cn(
              'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
              isActive('/') ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
            )}
          >
            <Home className="h-4 w-4" />
            <span>
              <Translate contentKey="global.menu.home">Home</Translate>
            </span>
          </Link>

          {/* Entities Menu */}
          {isAuthenticated && (
            <Suspense fallback={null}>
              <EntitiesMenuItems />
            </Suspense>
          )}

          {/* Administration Menu */}
          {isAuthenticated && isAdmin && (
            <div className="space-y-1 mt-6">
              <div className="px-3 py-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                <Translate contentKey="global.menu.admin.main">Administration</Translate>
              </div>

              <Link
                to="/admin/gateway"
                className={cn(
                  'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/admin/gateway')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <LayoutDashboard className="h-4 w-4" />
                <span>
                  <Translate contentKey="global.menu.admin.gateway">Gateway</Translate>
                </span>
              </Link>

              <Link
                to="/admin/tenant-management"
                className={cn(
                  'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/admin/tenant-management')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <Building2 className="h-4 w-4" />
                <span>
                  <Translate contentKey="global.menu.admin.tenantManagement">Tenant Management</Translate>
                </span>
              </Link>

              <Link
                to="/admin/metrics"
                className={cn(
                  'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/admin/metrics')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <Activity className="h-4 w-4" />
                <span>
                  <Translate contentKey="global.menu.admin.metrics">Metrics</Translate>
                </span>
              </Link>

              <Link
                to="/admin/health"
                className={cn(
                  'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/admin/health')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <Heart className="h-4 w-4" />
                <span>
                  <Translate contentKey="global.menu.admin.health">Health</Translate>
                </span>
              </Link>

              <Link
                to="/admin/configuration"
                className={cn(
                  'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/admin/configuration')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <Settings className="h-4 w-4" />
                <span>
                  <Translate contentKey="global.menu.admin.configuration">Configuration</Translate>
                </span>
              </Link>

              <Link
                to="/admin/logs"
                className={cn(
                  'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/admin/logs')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <FileText className="h-4 w-4" />
                <span>
                  <Translate contentKey="global.menu.admin.logs">Logs</Translate>
                </span>
              </Link>

              {isOpenAPIEnabled && (
                <Link
                  to="/admin/docs"
                  className={cn(
                    'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                    isActive('/admin/docs')
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                  )}
                >
                  <BookOpen className="h-4 w-4" />
                  <span>
                    <Translate contentKey="global.menu.admin.apidocs">API</Translate>
                  </span>
                </Link>
              )}
            </div>
          )}
        </nav>
      </div>
    </div>
  );
};

export default Sidebar;

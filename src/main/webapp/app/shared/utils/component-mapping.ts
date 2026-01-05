/**
 * Helper functions for mapping reactstrap props to shadcn variants
 */

export const mapBadgeColor = (color: string): string => {
  const colorMap: Record<string, string> = {
    success: 'default',
    danger: 'destructive',
    warning: 'secondary',
    info: 'outline',
    primary: 'default',
    secondary: 'secondary',
  };
  return colorMap[color] || 'default';
};

export const mapButtonColor = (color: string): string => {
  const colorMap: Record<string, string> = {
    primary: 'default',
    secondary: 'secondary',
    success: 'default',
    danger: 'destructive',
    warning: 'secondary',
    info: 'outline',
    link: 'link',
  };
  return colorMap[color] || 'default';
};

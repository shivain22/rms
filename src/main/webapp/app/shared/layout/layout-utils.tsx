import React from 'react';
import { cn } from '@/lib/utils';

/**
 * Row component to replace reactstrap Row
 * Uses Tailwind's flexbox utilities
 */
export const Row: React.FC<{
  children: React.ReactNode;
  className?: string;
}> = ({ children, className }) => {
  return <div className={cn('flex flex-wrap', className)}>{children}</div>;
};

/**
 * Col component to replace reactstrap Col
 * Uses Tailwind's grid/flex utilities
 *
 * @param md - Medium breakpoint column span (1-12)
 * @param sm - Small breakpoint column span (1-12)
 * @param lg - Large breakpoint column span (1-12)
 * @param xs - Extra small breakpoint column span (1-12)
 */
export const Col: React.FC<{
  children: React.ReactNode;
  className?: string;
  md?: number | string;
  sm?: number | string;
  lg?: number | string;
  xs?: number | string;
}> = ({ children, className, md, sm, lg, xs }) => {
  const colClasses: string[] = [];

  // Default: full width on mobile
  if (xs) {
    colClasses.push(`col-span-${xs}`);
  } else {
    colClasses.push('w-full');
  }

  // Small breakpoint
  if (sm) {
    colClasses.push(`sm:w-${sm}/12`);
  }

  // Medium breakpoint
  if (md) {
    colClasses.push(`md:w-${md}/12`);
  }

  // Large breakpoint
  if (lg) {
    colClasses.push(`lg:w-${lg}/12`);
  }

  return <div className={cn('flex', colClasses.join(' '), className)}>{children}</div>;
};

// Alternative: Simple Col using flex
export const ColFlex: React.FC<{
  children: React.ReactNode;
  className?: string;
  md?: number | string;
  sm?: number | string;
}> = ({ children, className, md, sm }) => {
  const widthClasses: string[] = [];

  if (md) {
    widthClasses.push(`md:w-${md}/12`);
  }
  if (sm) {
    widthClasses.push(`sm:w-${sm}/12`);
  }

  return <div className={cn('w-full', widthClasses.join(' '), className)}>{children}</div>;
};

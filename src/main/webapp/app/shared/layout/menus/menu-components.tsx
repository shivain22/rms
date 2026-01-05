import React from 'react';
import { IconProp } from '@fortawesome/fontawesome-svg-core';

import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { Button } from '@/components/ui/button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

export const NavDropdown = (props: {
  id?: string;
  'data-cy'?: string;
  icon: IconProp;
  name: string;
  children: React.ReactNode;
  style?: React.CSSProperties;
}) => (
  <DropdownMenu>
    <DropdownMenuTrigger asChild>
      <Button variant="ghost" className="flex items-center space-x-2" id={props.id} data-cy={props['data-cy']}>
        <FontAwesomeIcon icon={props.icon} />
        <span>{props.name}</span>
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" style={props.style}>
      {props.children}
    </DropdownMenuContent>
  </DropdownMenu>
);

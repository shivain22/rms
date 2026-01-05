import React from 'react';
import { DropdownMenuItem } from '@/components/ui/dropdown-menu';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';

export interface IMenuItem {
  children: React.ReactNode;
  icon: IconProp;
  to: string;
  id?: string;
  'data-cy'?: string;
}

const MenuItem = (props: IMenuItem) => {
  const { to, icon, id, children } = props;

  return (
    <DropdownMenuItem asChild>
      <Link to={to} id={id} data-cy={props['data-cy']} className="flex items-center space-x-2">
        <FontAwesomeIcon icon={icon} fixedWidth />
        <span>{children}</span>
      </Link>
    </DropdownMenuItem>
  );
};

export default MenuItem;

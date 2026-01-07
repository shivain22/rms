import React from 'react';
import MenuItem from 'app/shared/layout/menus/menu-item';
import { DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator } from '@/components/ui/dropdown-menu';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Translate, translate } from 'react-jhipster';
import { getLoginUrl } from 'app/shared/util/url-utils';
import { useLocation, useNavigate } from 'react-router';
import { NavDropdown } from './menu-components';

const accountMenuItemsAuthenticated = (account: any) => (
  <>
    <DropdownMenuLabel className="font-normal">
      <div className="flex flex-col space-y-1">
        <p className="text-sm font-medium leading-none">{account?.login || translate('global.menu.account.main')}</p>
        {account?.email && <p className="text-xs leading-none text-muted-foreground">{account.email}</p>}
      </div>
    </DropdownMenuLabel>
    <DropdownMenuSeparator />
    <MenuItem icon="sign-out-alt" to="/logout" data-cy="logout">
      <Translate contentKey="global.menu.account.logout">Sign out</Translate>
    </MenuItem>
  </>
);

const accountMenuItems = () => {
  const navigate = useNavigate();
  const pageLocation = useLocation();

  return (
    <>
      <DropdownMenuItem
        id="login-item"
        data-cy="login"
        onClick={() =>
          navigate(getLoginUrl(), {
            state: { from: pageLocation },
          })
        }
      >
        <FontAwesomeIcon icon="sign-in-alt" /> <Translate contentKey="global.menu.account.login">Sign in</Translate>
      </DropdownMenuItem>
    </>
  );
};

export const AccountMenu = ({ isAuthenticated = false, account = null }) => (
  <NavDropdown
    icon="user"
    name={isAuthenticated && account?.login ? account.login : translate('global.menu.account.main')}
    id="account-menu"
    data-cy="accountMenu"
  >
    {isAuthenticated && accountMenuItemsAuthenticated(account)}
    {!isAuthenticated && accountMenuItems()}
  </NavDropdown>
);

export default AccountMenu;

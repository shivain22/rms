import React from 'react';
import { DropdownMenuItem } from '@/components/ui/dropdown-menu';
import { languages, locales } from 'app/config/translation';
import { NavDropdown } from './menu-components';

export const LocaleMenu = ({ currentLocale, onClick }: { currentLocale: string; onClick: (event: any) => void }) =>
  Object.keys(languages).length > 1 ? (
    <NavDropdown icon="flag" name={currentLocale ? languages[currentLocale].name : undefined}>
      {locales.map(locale => (
        <DropdownMenuItem key={locale} onClick={() => onClick({ target: { value: locale } })}>
          {languages[locale].name}
        </DropdownMenuItem>
      ))}
    </NavDropdown>
  ) : null;

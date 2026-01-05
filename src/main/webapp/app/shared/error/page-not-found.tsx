import React from 'react';
import { Translate } from 'react-jhipster';
import { Alert, AlertDescription } from '@/components/ui/alert';

const PageNotFound = () => {
  return (
    <div>
      <Alert variant="destructive">
        <AlertDescription>
          <Translate contentKey="error.http.404">The page does not exist.</Translate>
        </AlertDescription>
      </Alert>
    </div>
  );
};

export default PageNotFound;

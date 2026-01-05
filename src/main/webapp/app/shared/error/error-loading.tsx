import React from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';

const ErrorLoading = () => {
  return (
    <div>
      <Alert variant="destructive">
        <AlertDescription>Error loading component</AlertDescription>
      </Alert>
    </div>
  );
};

export default ErrorLoading;

import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getTenant, deleteTenant } from './tenant-management.reducer';

export const TenantManagementDeleteDialog = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const [loadModal, setLoadModal] = useState(false);

  const tenant = useAppSelector(state => state.tenantManagement.tenant);
  const updateSuccess = useAppSelector(state => state.tenantManagement.updateSuccess);

  useEffect(() => {
    if (id) {
      dispatch(getTenant(id));
      setLoadModal(true);
    }
  }, [id]);

  useEffect(() => {
    if (updateSuccess && loadModal) {
      handleClose();
      setLoadModal(false);
    }
  }, [updateSuccess, loadModal]);

  const handleClose = () => {
    navigate('/admin/tenant-management');
  };

  const confirmDelete = () => {
    if (tenant.id) {
      dispatch(deleteTenant(tenant.id));
    }
  };

  return (
    <Dialog open={loadModal} onOpenChange={open => !open && handleClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle data-cy="tenantDeleteDialogHeading">
            <Translate contentKey="entity.delete.title">Confirm delete operation</Translate>
          </DialogTitle>
        </DialogHeader>
        <DialogDescription id="rmsApp.tenant.delete.question">
          <Translate contentKey="tenantManagement.delete.question" interpolate={{ id: tenant.tenantId }}>
            Are you sure you want to delete this Tenant?
          </Translate>
        </DialogDescription>
        <DialogFooter>
          <Button variant="secondary" onClick={handleClose}>
            <FontAwesomeIcon icon="ban" />
            &nbsp;
            <Translate contentKey="entity.action.cancel">Cancel</Translate>
          </Button>
          <Button id="jhi-confirm-delete-tenant" data-cy="entityConfirmDeleteButton" variant="destructive" onClick={confirmDelete}>
            <FontAwesomeIcon icon="trash" />
            &nbsp;
            <Translate contentKey="entity.action.delete">Delete</Translate>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default TenantManagementDeleteDialog;

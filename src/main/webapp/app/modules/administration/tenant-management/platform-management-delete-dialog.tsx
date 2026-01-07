import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getPlatform, deletePlatform } from './platform.reducer';

export const PlatformManagementDeleteDialog = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const [loadModal, setLoadModal] = useState(false);

  const platform = useAppSelector(state => state.platform.platforms.find(p => p.id?.toString() === id));
  const loading = useAppSelector(state => state.platform.loading);

  useEffect(() => {
    if (id) {
      dispatch(getPlatform(id));
      setLoadModal(true);
    }
  }, [id, dispatch]);

  const handleClose = () => {
    navigate('/admin/platform-management');
  };

  const confirmDelete = () => {
    if (platform?.id) {
      dispatch(deletePlatform(platform.id)).then(() => {
        handleClose();
      });
    }
  };

  return (
    <Dialog open={loadModal} onOpenChange={open => !open && handleClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle data-cy="platformDeleteDialogHeading">
            <Translate contentKey="entity.delete.title">Confirm delete operation</Translate>
          </DialogTitle>
        </DialogHeader>
        <DialogDescription id="rmsApp.platform.delete.question">
          <Translate contentKey="platformManagement.delete.question" interpolate={{ name: platform?.name }}>
            Are you sure you want to delete Platform &quot;{platform?.name}&quot;? This action cannot be undone.
          </Translate>
        </DialogDescription>
        <DialogFooter>
          <Button variant="secondary" onClick={handleClose} disabled={loading}>
            <FontAwesomeIcon icon="ban" />
            &nbsp;
            <Translate contentKey="entity.action.cancel">Cancel</Translate>
          </Button>
          <Button
            id="jhi-confirm-delete-platform"
            data-cy="entityConfirmDeleteButton"
            variant="destructive"
            onClick={confirmDelete}
            disabled={loading}
          >
            <FontAwesomeIcon icon="trash" />
            &nbsp;
            <Translate contentKey="entity.action.delete">Delete</Translate>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default PlatformManagementDeleteDialog;

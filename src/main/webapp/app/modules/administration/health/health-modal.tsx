import React from 'react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Translate } from 'react-jhipster';

const formatDiskSpaceOutput = rawValue => {
  // Should display storage space in a human readable unit
  const val = rawValue / 1073741824;
  if (val > 1) {
    // Value
    return `${val.toFixed(2)} GB`;
  }
  return `${(rawValue / 1048576).toFixed(2)} MB`;
};

interface HealthModalProps {
  handleClose: () => void;
  healthObject: any;
  showModal: boolean;
}

const HealthModal = ({ handleClose, healthObject, showModal }: HealthModalProps) => {
  const data = healthObject.details || {};
  return (
    <Dialog open={showModal} onOpenChange={open => !open && handleClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{healthObject.name}</DialogTitle>
        </DialogHeader>
        <DialogDescription>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  <Translate contentKey="health.details.name">Name</Translate>
                </TableHead>
                <TableHead>
                  <Translate contentKey="health.details.value">Value</Translate>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {Object.keys(data).map((key, index) => (
                <TableRow key={index}>
                  <TableCell>{key}</TableCell>
                  <TableCell>{healthObject.name === 'diskSpace' ? formatDiskSpaceOutput(data[key]) : JSON.stringify(data[key])}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </DialogDescription>
        <DialogFooter>
          <Button onClick={handleClose}>Close</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default HealthModal;

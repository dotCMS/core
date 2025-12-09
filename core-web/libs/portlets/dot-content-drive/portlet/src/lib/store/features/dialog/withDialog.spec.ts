import { describe, it, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { withDialog } from './withDialog';

import { DIALOG_TYPE } from '../../../shared/constants';
import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';

const initialState: DotContentDriveState = {
    currentSite: null,
    path: '',
    filters: {},
    items: [],
    selectedItems: [],
    status: DotContentDriveStatus.LOADING,
    totalItems: 0,
    pagination: { limit: 40, offset: 0 },
    sort: { field: 'modDate', order: DotContentDriveSortOrder.ASC },
    isTreeExpanded: true
};

export const dialogStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withDialog()
);

describe('withDialog', () => {
    let spectator: SpectatorService<InstanceType<typeof dialogStoreMock>>;
    let store: InstanceType<typeof dialogStoreMock>;

    const createService = createServiceFactory({
        service: dialogStoreMock
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('initial state', () => {
        it('should initialize with default context menu state', () => {
            expect(store.dialog()).toEqual(undefined);
        });
    });

    describe('methods', () => {
        const mockDialog = {
            type: DIALOG_TYPE.FOLDER,
            header: 'Folder'
        };

        it('should set the dialog state', () => {
            store.setDialog(mockDialog);
            expect(store.dialog()).toEqual(mockDialog);
        });

        it('should reset the dialog state', () => {
            store.setDialog(mockDialog);
            store.closeDialog();
            expect(store.dialog()).toEqual(undefined);
        });
    });
});

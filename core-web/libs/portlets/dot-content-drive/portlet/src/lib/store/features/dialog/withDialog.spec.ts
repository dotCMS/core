import { describe, it, expect } from '@jest/globals';
import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

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
    pagination: { limit: 40, page: 1, offset: 0 },
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

        it('should initialize with no drill-down header', () => {
            expect(store.dialogDrillDown()).toEqual(undefined);
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

    /**
     * Header override published by a dialog body that has drilled into a sub-screen, so the shell's
     * one header retitles instead of the body rendering a second one.
     */
    describe('drill-down header', () => {
        const drillDown = { header: 'Send for Review', itemCount: 2 };
        const mockDialog = {
            type: DIALOG_TYPE.ACTION_CENTER,
            header: 'Workflow Center'
        };

        it('should set the drill-down header', () => {
            store.setDialogDrillDown(drillDown);

            expect(store.dialogDrillDown()).toEqual(drillDown);
        });

        it('should clear the drill-down header', () => {
            store.setDialogDrillDown(drillDown);
            store.clearDialogDrillDown();

            expect(store.dialogDrillDown()).toEqual(undefined);
        });

        it('should keep the dialog itself untouched', () => {
            store.setDialog(mockDialog);
            store.setDialogDrillDown(drillDown);

            expect(store.dialog()).toEqual(mockDialog);
        });

        it('should drop a stale drill-down when a new dialog opens', () => {
            // Otherwise the next dialog would open already titled by the previous one's sub-screen.
            store.setDialogDrillDown(drillDown);
            store.setDialog(mockDialog);

            expect(store.dialogDrillDown()).toEqual(undefined);
        });

        it('should drop the drill-down on close', () => {
            store.setDialogDrillDown(drillDown);
            store.closeDialog();

            expect(store.dialogDrillDown()).toEqual(undefined);
        });
    });
});

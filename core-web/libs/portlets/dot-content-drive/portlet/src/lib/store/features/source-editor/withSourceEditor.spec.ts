import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { describe, expect, it } from 'vitest';

import { DotSourceEditorRequest } from '@dotcms/edit-content';

import { withSourceEditor } from './withSourceEditor';

import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';

const initialState = {
    currentSite: null,
    path: '',
    filters: {},
    items: [],
    selectedItems: [],
    status: DotContentDriveStatus.LOADING,
    pagination: { limit: 40, offset: 0 },
    sort: { field: 'modDate', order: DotContentDriveSortOrder.ASC },
    isTreeExpanded: true
} as unknown as DotContentDriveState;

const sourceEditorStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withSourceEditor()
);

const request: DotSourceEditorRequest = {
    inode: 'inode-1',
    identifier: 'identifier-1',
    languageId: 1,
    title: 'header.vtl',
    language: 'velocity'
};

describe('withSourceEditor', () => {
    let spectator: SpectatorService<InstanceType<typeof sourceEditorStoreMock>>;
    let store: InstanceType<typeof sourceEditorStoreMock>;

    const createService = createServiceFactory({ service: sourceEditorStoreMock });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should start with the panel closed', () => {
        expect(store.sourceEditor()).toBeNull();
    });

    it('should open the panel on the requested file', () => {
        store.openSourceEditor(request);

        expect(store.sourceEditor()).toEqual(request);
    });

    it('should close the panel', () => {
        store.openSourceEditor(request);
        store.closeSourceEditor();

        expect(store.sourceEditor()).toBeNull();
    });
});

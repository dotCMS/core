import { describe, expect, it } from '@jest/globals';
import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import { PushPublishService } from '@dotcms/data-access';
import { DotEnvironment } from '@dotcms/dotcms-models';

import { withPushPublishEnvironments } from './withPushPublishEnvironments';

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

const environment = (id: string): DotEnvironment => ({ id, name: `env-${id}` }) as DotEnvironment;

const pushPublishEnvironmentsStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withPushPublishEnvironments()
);

describe('withPushPublishEnvironments', () => {
    let spectator: SpectatorService<InstanceType<typeof pushPublishEnvironmentsStoreMock>>;
    let store: InstanceType<typeof pushPublishEnvironmentsStoreMock>;

    // Shared across store creations so a test can set the answer before the store initialises.
    const getEnvironments = jest.fn();

    const createService = createServiceFactory({
        service: pushPublishEnvironmentsStoreMock,
        providers: [mockProvider(PushPublishService, { getEnvironments })]
    });

    // Never emits: the lookup is in flight, which is what the third state stands for.
    const build = (environments = NEVER) => {
        getEnvironments.mockReturnValue(environments);
        spectator = createService();
        store = spectator.service;
    };

    beforeEach(() => {
        getEnvironments.mockReset();
    });

    it('should look the environments up on init, with no explicit call', () => {
        build();

        expect(getEnvironments).toHaveBeenCalled();
    });

    it('should stay undefined while the lookup is in flight, which is distinct from false', () => {
        build();

        expect(store.hasPushPublishEnvironments()).toBeUndefined();
    });

    it('should settle on true when an environment is reachable', () => {
        build(of([environment('a'), environment('b')]));

        expect(store.hasPushPublishEnvironments()).toBe(true);
    });

    it('should settle on false for an empty list', () => {
        build(of([]));

        expect(store.hasPushPublishEnvironments()).toBe(false);
    });

    // A failed lookup reads as "none" rather than "probably fine": offering a push with nowhere to
    // send it fails at the servlet with a message the user cannot act on.
    it('should settle on false when the lookup fails', () => {
        build(throwError(() => new Error('lookup failed')));

        expect(store.hasPushPublishEnvironments()).toBe(false);
    });

    it('should re-run the lookup when asked again, so a failure can be retried', () => {
        build(throwError(() => new Error('lookup failed')));
        expect(store.hasPushPublishEnvironments()).toBe(false);

        getEnvironments.mockReturnValue(of([environment('a')]));
        store.loadPushPublishEnvironments();

        expect(store.hasPushPublishEnvironments()).toBe(true);
    });
});

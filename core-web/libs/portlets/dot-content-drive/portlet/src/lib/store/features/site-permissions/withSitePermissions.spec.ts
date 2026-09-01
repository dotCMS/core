import { describe, expect, it } from '@jest/globals';
import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import { DotPermissionsService } from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';

import { withSitePermissions } from './withSitePermissions';

import { SYSTEM_HOST } from '../../../shared/constants';
import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';

const site = (identifier: string): DotSite =>
    ({ identifier, hostname: 'demo.dotcms.com' }) as DotSite;

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

const sitePermissionsStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withSitePermissions()
);

describe('withSitePermissions', () => {
    let spectator: SpectatorService<InstanceType<typeof sitePermissionsStoreMock>>;
    let store: InstanceType<typeof sitePermissionsStoreMock>;

    const canAddChildren = jest.fn();

    const createService = createServiceFactory({
        service: sitePermissionsStoreMock,
        providers: [mockProvider(DotPermissionsService, { canAddChildren })]
    });

    // Never emits: the lookup is in flight, which is what the undefined state stands for.
    const build = (response = NEVER) => {
        canAddChildren.mockReturnValue(response);
        spectator = createService();
        store = spectator.service;
    };

    beforeEach(() => {
        canAddChildren.mockReset();
    });

    it('should start undefined, before any site is known', () => {
        build();

        expect(store.siteCanAddChildren()).toBeUndefined();
    });

    it('should not look anything up until a site is set', () => {
        build();

        expect(canAddChildren).not.toHaveBeenCalled();
    });

    describe('loadSitePermissions', () => {
        it('should resolve true when the user can add children to the site', () => {
            build(of(true));

            store.loadSitePermissions(site('site-123'));

            expect(store.siteCanAddChildren()).toBe(true);
        });

        it('should resolve false when the user cannot add children to the site', () => {
            build(of(false));

            store.loadSitePermissions(site('site-123'));

            expect(store.siteCanAddChildren()).toBe(false);
        });

        it('should query the permissions endpoint with the site identifier', () => {
            build(of(true));

            store.loadSitePermissions(site('site-123'));

            expect(canAddChildren).toHaveBeenCalledWith('site-123');
        });

        // The gate softens a UI affordance; it does not protect the write. Denying on a failed
        // lookup would strip the creation buttons over a transient network error, so a failure
        // settles on "allowed" and lets the server refuse.
        it('should settle on true when the lookup fails', () => {
            build(throwError(() => new Error('boom')));

            store.loadSitePermissions(site('site-123'));

            expect(store.siteCanAddChildren()).toBe(true);
        });

        // The drive seeds `currentSite` with SYSTEM_HOST before a real site resolves, and the
        // pseudo-site has no meaningful root to gate on. Asking about it would answer for the wrong
        // asset entirely.
        it('should skip the lookup for SYSTEM_HOST', () => {
            build(of(false));

            store.loadSitePermissions(SYSTEM_HOST);

            expect(canAddChildren).not.toHaveBeenCalled();
            expect(store.siteCanAddChildren()).toBeUndefined();
        });

        it('should skip the lookup when no site is set', () => {
            build(of(false));

            store.loadSitePermissions(null);

            expect(canAddChildren).not.toHaveBeenCalled();
            expect(store.siteCanAddChildren()).toBeUndefined();
        });

        it('should reset to undefined while a new site is resolving', () => {
            build(of(false));
            store.loadSitePermissions(site('site-123'));
            expect(store.siteCanAddChildren()).toBe(false);

            canAddChildren.mockReturnValue(NEVER);
            store.loadSitePermissions(site('site-456'));

            expect(store.siteCanAddChildren()).toBeUndefined();
        });
    });
});

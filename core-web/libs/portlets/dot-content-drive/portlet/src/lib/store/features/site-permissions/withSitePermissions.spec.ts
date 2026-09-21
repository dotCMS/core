import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotPermissionsService } from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';

import { withSitePermissions } from './withSitePermissions';

import { SYSTEM_HOST } from '../../../shared/constants';
import { DotContentDriveState } from '../../../shared/models';
import { DOT_CONTENT_DRIVE_INITIAL_STATE } from '../../dot-content-drive.store';

const site = (identifier: string): DotSite =>
    ({ identifier, hostname: 'demo.dotcms.com' }) as DotSite;

const initialState: DotContentDriveState = {
    // Seeded from the store's own initial state so this fixture cannot drift from it; only the
    // keys this feature's tests care about are overridden.
    ...DOT_CONTENT_DRIVE_INITIAL_STATE,
    path: '',
    isTreeExpanded: true
};

const sitePermissionsStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withSitePermissions()
);

describe('withSitePermissions', () => {
    let spectator: SpectatorService<InstanceType<typeof sitePermissionsStoreMock>>;
    let store: InstanceType<typeof sitePermissionsStoreMock>;

    const canAddChildren = vi.fn();

    const createService = createServiceFactory({
        service: sitePermissionsStoreMock,
        providers: [mockProvider(DotPermissionsService, { canAddChildren })]
    });

    // Never emits: the lookup is in flight, which is what the undefined state stands for.
    const build = (response: Observable<boolean> = NEVER) => {
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

describe('withSitePermissions — reading System Host', () => {
    let spectator: SpectatorService<InstanceType<typeof sitePermissionsStoreMock>>;
    let store: InstanceType<typeof sitePermissionsStoreMock>;

    const canAddChildren = vi.fn();

    const createService = createServiceFactory({
        service: sitePermissionsStoreMock,
        providers: [mockProvider(DotPermissionsService, { canAddChildren })]
    });

    beforeEach(() => {
        canAddChildren.mockReset();
    });

    it('should treat a refusal as not readable', () => {
        // The permissions resource checks READ before it answers and refuses outright when the
        // caller does not hold it, so a 403 from this one call is the server saying the user
        // cannot see the asset at all -- not merely that they cannot add to it.
        canAddChildren.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
        spectator = createService();
        store = spectator.service;

        store.loadSystemHostPermissions();

        expect(store.systemHostCanRead()).toBe(false);
    });

    it('should keep System Host readable when the lookup fails for any other reason', () => {
        // A timeout or a 500 says nothing about permissions. Locking someone out of a scope
        // because the network hiccuped is worse than showing them an entry the server will
        // police anyway -- the listing enforces read permissions on its own.
        canAddChildren.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        spectator = createService();
        store = spectator.service;

        store.loadSystemHostPermissions();

        expect(store.systemHostCanRead()).toBe(true);
    });

    it('should keep System Host readable when the lookup succeeds', () => {
        canAddChildren.mockReturnValue(of(false));
        spectator = createService();
        store = spectator.service;

        store.loadSystemHostPermissions();

        // Answering the add question at all means the read check upstream of it passed.
        expect(store.systemHostCanRead()).toBe(true);
        expect(store.systemHostCanAddChildren()).toBe(false);
    });
});

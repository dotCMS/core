import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';

import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { signal } from '@angular/core';

import { DotClusterService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCluster, DotClusterNodes } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotNetworkStore } from './dot-network.store';

import { createDownNode, createNode } from '../../testing/dot-network.mocks';

const CURRENT = createNode({ serverId: 'current', displayServerId: 'current' });
const OTHER = createNode({ serverId: 'other', displayServerId: 'other' });
const DOWN = createDownNode({ serverId: 'down', displayServerId: 'down' });

const RESPONSE: DotClusterNodes = {
    currentServerId: 'current',
    nodes: [OTHER, DOWN, CURRENT]
};

/** `appconfiguration` → `cluster`, as the GlobalStore exposes it (#38032 AC-012). */
const systemCluster = signal<(DotCluster & { heartbeatEnabled?: boolean }) | null>({
    clusterId: 'c1',
    companyKeyDigest: 'k',
    heartbeatEnabled: true
});

describe('DotNetworkStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotNetworkStore>>;
    let store: InstanceType<typeof DotNetworkStore>;
    let clusterService: DotClusterService;

    const createService = createServiceFactory({
        service: DotNetworkStore,
        providers: [
            mockProvider(DotHttpErrorManagerService),
            // A getter, so the store reads the live signal (same shape as dot-ai.store.spec).
            mockProvider(GlobalStore, {
                get systemCluster() {
                    return systemCluster;
                }
            })
        ]
    });

    beforeEach(() => {
        systemCluster.set({ clusterId: 'c1', companyKeyDigest: 'k', heartbeatEnabled: true });
        // Fresh mocks per test: call counts and return values must not leak between tests.
        spectator = createService({
            providers: [
                mockProvider(DotClusterService, {
                    getNodes: vi.fn().mockReturnValue(of(RESPONSE))
                })
            ]
        });
        store = spectator.service;
        clusterService = spectator.inject(DotClusterService);
        spectator.flushEffects();
    });

    it('should load the nodes on init and select the node serving the request', () => {
        expect(clusterService.getNodes).toHaveBeenCalledTimes(1);
        expect(store.status()).toBe('LOADED');
        expect(store.selectedServerId()).toBe('current');
        expect(store.selectedNode()).toEqual(CURRENT);
    });

    it('should list the node serving the request first and keep the rest in order', () => {
        expect(store.sortedNodes().map((n) => n.serverId)).toEqual(['current', 'other', 'down']);
    });

    it('should change the selection', () => {
        store.selectNode('down');

        expect(store.selectedNode()).toEqual(DOWN);
    });

    it('should keep the selection on refresh when the node is still listed', () => {
        store.selectNode('other');
        store.load();

        expect(clusterService.getNodes).toHaveBeenCalledTimes(2);
        expect(store.selectedServerId()).toBe('other');
    });

    it('should fall back to the node serving the request when the selection disappears', () => {
        store.selectNode('down');
        vi.mocked(clusterService.getNodes).mockReturnValue(
            of({ ...RESPONSE, nodes: [CURRENT, OTHER] })
        );

        store.load();

        expect(store.selectedServerId()).toBe('current');
    });

    it('should keep the data on screen while refreshing', () => {
        const pending = new Subject<DotClusterNodes>();
        vi.mocked(clusterService.getNodes).mockReturnValue(pending);

        store.load();

        expect(store.isRefreshing()).toBe(true);
        expect(store.showSkeleton()).toBe(false);
        expect(store.nodes()).toHaveLength(3);

        pending.next(RESPONSE);
        expect(store.isRefreshing()).toBe(false);
    });

    it('should never start a second status request while one is in flight', () => {
        // Each status request makes every node write a test file to the shared assets
        // volume, so a refresh pressed twice must not cost the cluster twice (FR-039, SC-005).
        const pending = new Subject<DotClusterNodes>();
        vi.mocked(clusterService.getNodes).mockClear().mockReturnValue(pending);

        store.load();
        store.load();
        store.load();

        expect(clusterService.getNodes).toHaveBeenCalledTimes(1);

        pending.next(RESPONSE);
        pending.complete();
        vi.mocked(clusterService.getNodes).mockReturnValue(of(RESPONSE));
        store.load();

        expect(clusterService.getNodes).toHaveBeenCalledTimes(2);
    });

    it('should show the empty state when no node is listed', () => {
        vi.mocked(clusterService.getNodes).mockReturnValue(
            of({
                heartbeatEnabled: true,
                currentServerId: 'x',
                nodes: []
            })
        );

        store.load();

        expect(store.showEmpty()).toBe(true);
        expect(store.showUnlicensed()).toBe(false);
    });

    describe('heartbeat notice (FR-047), read from the app configuration', () => {
        it('should show the notice when the app configuration reports the heartbeat feature off', () => {
            systemCluster.set({ clusterId: 'c1', companyKeyDigest: 'k', heartbeatEnabled: false });

            expect(store.showHeartbeatNotice()).toBe(true);
        });

        it('should not show the notice when the heartbeat feature is on', () => {
            expect(store.showHeartbeatNotice()).toBe(false);
        });

        it('should treat a missing flag (older instance) as on', () => {
            systemCluster.set({ clusterId: 'c1', companyKeyDigest: 'k' });

            expect(store.showHeartbeatNotice()).toBe(false);
        });

        it('should ignore any heartbeat value in the node response', () => {
            vi.mocked(clusterService.getNodes).mockReturnValue(
                of({ ...RESPONSE, heartbeatEnabled: false } as DotClusterNodes)
            );

            store.load();

            expect(store.showHeartbeatNotice()).toBe(false);
        });

        it('should not show the notice when there are no nodes', () => {
            systemCluster.set({ clusterId: 'c1', companyKeyDigest: 'k', heartbeatEnabled: false });
            vi.mocked(clusterService.getNodes).mockReturnValue(of({ ...RESPONSE, nodes: [] }));

            store.load();

            expect(store.showHeartbeatNotice()).toBe(false);
        });
    });

    describe('on error', () => {
        const error = new HttpErrorResponse({ status: 500 });

        it('should report the error and keep the previous nodes on a failed refresh', () => {
            vi.mocked(clusterService.getNodes).mockReturnValue(throwError(() => error));

            store.load();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
            expect(store.status()).toBe('ERROR');
            expect(store.nodes()).toHaveLength(3);
            expect(store.showError()).toBe(false);
        });
    });

    describe('license refusal (FR-045)', () => {
        const licenseError = new HttpErrorResponse({
            status: 403,
            headers: new HttpHeaders({ 'error-key': 'dotcms.api.error.license.required' })
        });

        it('should show the unlicensed state on a license 403, without the shared error handler', () => {
            vi.mocked(clusterService.getNodes).mockReturnValue(throwError(() => licenseError));

            store.load();

            expect(store.showUnlicensed()).toBe(true);
            expect(store.showError()).toBe(false);
            expect(store.nodes()).toEqual([]);
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
        });

        it('should treat a 403 without the license error key as an error, not as unlicensed', () => {
            const forbidden = new HttpErrorResponse({ status: 403 });
            vi.mocked(clusterService.getNodes).mockReturnValue(throwError(() => forbidden));

            store.load();

            expect(store.showUnlicensed()).toBe(false);
            expect(store.status()).toBe('ERROR');
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(
                forbidden
            );
        });
    });
});

describe('DotNetworkStore — first load fails', () => {
    const createService = createServiceFactory({
        service: DotNetworkStore,
        providers: [
            mockProvider(DotClusterService, {
                getNodes: vi
                    .fn()
                    .mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(GlobalStore, {
                get systemCluster() {
                    return systemCluster;
                }
            })
        ]
    });

    it('should show the error state', () => {
        const spectator = createService();
        spectator.flushEffects();

        expect(spectator.service.showError()).toBe(true);
        expect(spectator.service.showSkeleton()).toBe(false);
    });
});

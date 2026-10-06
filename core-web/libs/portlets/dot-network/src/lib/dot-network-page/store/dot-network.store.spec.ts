import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotClusterService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotClusterNodes } from '@dotcms/dotcms-models';

import { DotNetworkStore } from './dot-network.store';

import { createDownNode, createNode } from '../../testing/dot-network.mocks';

const CURRENT = createNode({ serverId: 'current', displayServerId: 'current' });
const OTHER = createNode({ serverId: 'other', displayServerId: 'other' });
const DOWN = createDownNode({ serverId: 'down', displayServerId: 'down' });

const RESPONSE: DotClusterNodes = {
    licensed: true,
    currentServerId: 'current',
    clusterHealth: 'RED',
    nodes: [OTHER, DOWN, CURRENT]
};

describe('DotNetworkStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotNetworkStore>>;
    let store: InstanceType<typeof DotNetworkStore>;
    let clusterService: DotClusterService;

    const createService = createServiceFactory({
        service: DotNetworkStore,
        providers: [mockProvider(DotHttpErrorManagerService)]
    });

    beforeEach(() => {
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

    it('should show the empty state when no node is listed', () => {
        vi.mocked(clusterService.getNodes).mockReturnValue(
            of({ licensed: true, currentServerId: 'x', clusterHealth: 'GREEN', nodes: [] })
        );

        store.load();

        expect(store.showEmpty()).toBe(true);
        expect(store.showUnlicensed()).toBe(false);
    });

    it('should show the unlicensed state', () => {
        vi.mocked(clusterService.getNodes).mockReturnValue(
            of({ licensed: false, currentServerId: '', clusterHealth: 'GREEN', nodes: [] })
        );

        store.load();

        expect(store.showUnlicensed()).toBe(true);
        expect(store.showEmpty()).toBe(false);
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
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    it('should show the error state', () => {
        const spectator = createService();
        spectator.flushEffects();

        expect(spectator.service.showError()).toBe(true);
        expect(spectator.service.showSkeleton()).toBe(false);
    });
});

import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNetworkPageComponent } from './dot-network-page.component';
import { DotNetworkStore } from './store/dot-network.store';

import { createNode } from '../testing/dot-network.mocks';

const MESSAGES = {
    'network.retry': 'Try again',
    'network.empty.title': 'No nodes found',
    'network.error.title': 'Cluster status could not be loaded',
    'network.unlicensed.title': 'Network requires an Enterprise license'
};

const NODE = createNode({ serverId: 'current' });

const storeMock = () =>
    mockProvider(DotNetworkStore, {
        showSkeleton: vi.fn().mockReturnValue(false),
        showUnlicensed: vi.fn().mockReturnValue(false),
        showError: vi.fn().mockReturnValue(false),
        showEmpty: vi.fn().mockReturnValue(false),
        selectedNode: vi.fn().mockReturnValue(NODE),
        sortedNodes: vi.fn().mockReturnValue([NODE]),
        selectedServerId: vi.fn().mockReturnValue('current'),
        currentServerId: vi.fn().mockReturnValue('current'),
        isRefreshing: vi.fn().mockReturnValue(false),
        load: vi.fn(),
        selectNode: vi.fn()
    });

const FLAGS = ['showSkeleton', 'showUnlicensed', 'showError', 'showEmpty'] as const;
type StoreFlag = (typeof FLAGS)[number];

describe('DotNetworkPageComponent', () => {
    let spectator: Spectator<DotNetworkPageComponent>;

    const createComponent = createComponentFactory({
        component: DotNetworkPageComponent,
        componentProviders: [storeMock()],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }],
        detectChanges: false
    });

    function setup(flag?: StoreFlag) {
        spectator = createComponent();
        const store = spectator.inject(DotNetworkStore, true);
        // The mock is shared across tests, so every flag is reset before the one under test.
        for (const name of FLAGS) {
            vi.mocked(store[name]).mockReturnValue(false);
        }

        if (flag) {
            vi.mocked(store[flag]).mockReturnValue(true);
        }

        spectator.detectChanges();

        return store;
    }

    it('should render the node list and the selected node', () => {
        setup();

        expect(spectator.query(byTestId('network-node-list'))).toExist();
        expect(spectator.query(byTestId('network-node-detail'))).toExist();
    });

    it('should forward a card selection to the store', () => {
        const store = setup();

        spectator.triggerEventHandler('dot-network-node-list', 'selectNode', 'other');

        expect(store.selectNode).toHaveBeenCalledWith('other');
    });

    it('should reload from the detail Refresh button', () => {
        const store = setup();

        spectator.triggerEventHandler('dot-network-node-detail', 'refresh', undefined);

        expect(store.load).toHaveBeenCalled();
    });

    it('should show the skeleton on the first load', () => {
        setup('showSkeleton');

        expect(spectator.query(byTestId('network-loading'))).toExist();
        expect(spectator.query(byTestId('network-node-list'))).toBeNull();
    });

    it('should show the unlicensed state', () => {
        setup('showUnlicensed');

        expect(spectator.query(byTestId('network-unlicensed'))).toContainText(
            'Network requires an Enterprise license'
        );
    });

    it('should show the error state with a retry', () => {
        const store = setup('showError');

        expect(spectator.query(byTestId('network-error'))).toContainText(
            'Cluster status could not be loaded'
        );
        spectator.click(byTestId('message-button'));
        expect(store.load).toHaveBeenCalled();
    });

    it('should show the empty state', () => {
        setup('showEmpty');

        expect(spectator.query(byTestId('network-empty'))).toContainText('No nodes found');
    });
});

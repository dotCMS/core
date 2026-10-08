import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { BehaviorSubject } from 'rxjs';
import { vi } from 'vitest';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNetworkPageComponent } from './dot-network-page.component';
import { DotNetworkStore } from './store/dot-network.store';

import { createNode } from '../testing/dot-network.mocks';

const MESSAGES = {
    'network.retry': 'Try again',
    'network.empty.title': 'No nodes found',
    'network.error.title': 'Cluster status could not be loaded',
    'network.heartbeat.disabled': 'The server heartbeat feature is disabled.',
    'com.dotcms.repackage.javax.portlet.title.EXT_CLUSTERING_TOOL': 'Clustering',
    'only-available-in': 'is only available in'
};

/** What the Clustering upsell is fed (FR-046, research.md R-2). */
const CLUSTERING_UPSELL = {
    icon: 'server',
    titleKey: 'com.dotcms.repackage.javax.portlet.title.EXT_CLUSTERING_TOOL',
    url: '/c/network-beta'
};

const NODE = createNode({ serverId: 'current' });

const storeMock = () =>
    mockProvider(DotNetworkStore, {
        showSkeleton: vi.fn().mockReturnValue(false),
        showUnlicensed: vi.fn().mockReturnValue(false),
        showError: vi.fn().mockReturnValue(false),
        showEmpty: vi.fn().mockReturnValue(false),
        showHeartbeatNotice: vi.fn().mockReturnValue(false),
        selectedNode: vi.fn().mockReturnValue(NODE),
        sortedNodes: vi.fn().mockReturnValue([NODE]),
        selectedServerId: vi.fn().mockReturnValue('current'),
        currentServerId: vi.fn().mockReturnValue('current'),
        isRefreshing: vi.fn().mockReturnValue(false),
        load: vi.fn(),
        selectNode: vi.fn()
    });

const FLAGS = [
    'showSkeleton',
    'showUnlicensed',
    'showError',
    'showEmpty',
    'showHeartbeatNotice'
] as const;
type StoreFlag = (typeof FLAGS)[number];

describe('DotNetworkPageComponent', () => {
    let spectator: Spectator<DotNetworkPageComponent>;
    const licenseService = {
        unlicenseData: new BehaviorSubject({ icon: '', titleKey: '', url: '' })
    };

    const createComponent = createComponentFactory({
        component: DotNetworkPageComponent,
        componentProviders: [storeMock()],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            { provide: DotLicenseService, useValue: licenseService }
        ],
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

    describe('unlicensed install (FR-046)', () => {
        beforeEach(() => {
            licenseService.unlicenseData.next({ icon: '', titleKey: '', url: '' });
        });

        it('should show the Clustering upsell instead of the empty-state message', () => {
            const next = vi.spyOn(licenseService.unlicenseData, 'next');
            setup('showUnlicensed');

            expect(next).toHaveBeenCalledWith(CLUSTERING_UPSELL);
            const unlicensed = spectator.query(byTestId('network-unlicensed'));
            expect(unlicensed?.querySelector('dot-not-license')).toExist();
            expect(unlicensed?.querySelector('[data-testid="title"]')).toHaveText('Clustering');
            expect(spectator.query('dot-empty-container')).toBeNull();
            next.mockRestore();
        });
    });

    describe('heartbeat feature off (FR-047)', () => {
        it('should show the notice above the node list and keep the data visible', () => {
            setup('showHeartbeatNotice');

            const notice = spectator.query(byTestId('network-heartbeat-notice'));
            const list = spectator.query(byTestId('network-node-list'));
            expect(notice).toContainText('The server heartbeat feature is disabled.');
            expect(list).toExist();
            expect(spectator.query(byTestId('network-node-detail'))).toExist();
            expect(
                notice &&
                    list &&
                    notice.compareDocumentPosition(list) & Node.DOCUMENT_POSITION_FOLLOWING
            ).toBeTruthy();
        });

        it('should not show the notice when the heartbeat feature is on', () => {
            setup();

            expect(spectator.query(byTestId('network-heartbeat-notice'))).toBeNull();
        });
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

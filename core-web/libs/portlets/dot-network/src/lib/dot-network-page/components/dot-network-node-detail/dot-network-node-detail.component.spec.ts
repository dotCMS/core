import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNetworkNodeDetailComponent } from './dot-network-node-detail.component';

import { createDownNode, createNode } from '../../../testing/dot-network.mocks';

const MESSAGES = {
    'network.refresh': 'Refresh',
    'network.status.up': 'Up',
    'network.status.down': 'Down',
    'network.node.this-node': 'This node',
    'network.node.not-responding': 'This node did not answer',
    'network.value.not-applicable': 'N/A',
    'network.value.milliseconds': '{0} ms',
    'network.cache.subtitle': 'Cluster {0}',
    'network.assets.subtitle': 'Shared volume',
    'network.search.engine.opensearch': 'OpenSearch',
    'network.cache.port': 'Cache Port',
    'network.search.timed-out': 'Timed Out',
    'network.search.number-of-data-nodes': 'Number of Data Nodes'
};

/** Values of a section, keyed by row label key, read from the rendered DOM. */
function sectionValues(spectator: Spectator<DotNetworkNodeDetailComponent>, testId: string) {
    const section = spectator.query(byTestId(testId)) as HTMLElement;

    return Array.from(section.querySelectorAll('[data-testid="network-section-value"]')).map((el) =>
        el.textContent?.trim()
    );
}

describe('DotNetworkNodeDetailComponent', () => {
    let spectator: Spectator<DotNetworkNodeDetailComponent>;

    const createComponent = createComponentFactory({
        component: DotNetworkNodeDetailComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('node', createNode());
        spectator.detectChanges();
    });

    describe('header', () => {
        it('should show the server id, status and name', () => {
            expect(spectator.query(byTestId('network-detail-id'))).toHaveText('10b10fa5');
            expect(spectator.query(byTestId('network-detail-status'))).toHaveText('Up');
            expect(spectator.query(byTestId('network-detail-name'))).toHaveText(
                'dotcms-demo-56cc78659b-kgzws'
            );
        });

        it('should show "This node" only for the node serving the request', () => {
            expect(spectator.query(byTestId('network-detail-current'))).toBeNull();

            spectator.setInput('isCurrent', true);

            expect(spectator.query(byTestId('network-detail-current'))).toHaveText('This node');
        });

        it('should emit refresh', () => {
            let emitted = false;
            spectator.output('refresh').subscribe(() => (emitted = true));

            spectator.click(spectator.query('[data-testid="network-refresh"] button') as Element);

            expect(emitted).toBe(true);
        });

        it('should put the Refresh button in its loading state while refreshing', () => {
            spectator.setInput('refreshing', true);

            expect(
                spectator.query('[data-testid="network-refresh"] button')?.hasAttribute('disabled')
            ).toBe(true);
        });
    });

    it('should show the four summary tiles', () => {
        expect(
            spectator.queryAll(byTestId('network-tile-value')).map((el) => el.textContent?.trim())
        ).toEqual(['10b10fa5', 'dotcms-demo', '10.2.66.19', '26.09.18-01']);
    });

    describe('Cache Transport', () => {
        it('should show every row, with received and sent bytes and N/A for no port', () => {
            expect(sectionValues(spectator, 'network-section-cache')).toEqual([
                'dotcms-demo',
                'PubSub',
                '3',
                'true',
                'N/A',
                '181314 / 175002',
                'N/A'
            ]);
        });

        it('should show the cluster name as the subtitle', () => {
            const section = spectator.query(byTestId('network-section-cache')) as HTMLElement;

            expect(section.querySelector('[data-testid="network-section-subtitle"]')).toHaveText(
                'Cluster dotcms-demo'
            );
        });
    });

    describe('Search Cluster', () => {
        it('should bind Timed Out and Number of Data Nodes to their own values', () => {
            const values = sectionValues(spectator, 'network-section-search');

            expect(values[1]).toBe('false');
            expect(values[3]).toBe('2');
        });

        it('should show all fourteen rows', () => {
            expect(sectionValues(spectator, 'network-section-search')).toEqual([
                'opensearch-ovh-east',
                'false',
                '3',
                '2',
                '293',
                '589',
                '0',
                '0',
                '0',
                '0',
                '0',
                '0',
                '0 ms',
                '100%'
            ]);
        });

        it('should label the search engine when the backend reports it', () => {
            const section = () =>
                spectator.query(byTestId('network-section-search')) as HTMLElement;
            expect(section().querySelector('[data-testid="network-section-subtitle"]')).toBeNull();

            spectator.setInput(
                'node',
                createNode({ search: { ...createNode().search, engine: 'OPENSEARCH' } })
            );

            expect(section().querySelector('[data-testid="network-section-subtitle"]')).toHaveText(
                'OpenSearch'
            );
        });
    });

    it('should show the assets path and the read and write flags', () => {
        expect(sectionValues(spectator, 'network-section-assets')).toEqual([
            '/data/shared/assets',
            'true',
            'true'
        ]);
    });

    describe('node that did not answer', () => {
        beforeEach(() => spectator.setInput('node', createDownNode()));

        it('should show its identity, a Down badge and a warning', () => {
            expect(spectator.query(byTestId('network-detail-id'))).toHaveText('e93aa517');
            expect(spectator.query(byTestId('network-detail-status'))).toHaveText('Down');
            expect(spectator.query(byTestId('network-detail-not-responding'))).toExist();
        });

        it('should show empty values instead of an error', () => {
            const cache = sectionValues(spectator, 'network-section-cache');

            expect(cache.every((value) => value === '—')).toBe(true);
            expect(sectionValues(spectator, 'network-section-search').every((v) => v === '—')).toBe(
                true
            );
        });
    });
});

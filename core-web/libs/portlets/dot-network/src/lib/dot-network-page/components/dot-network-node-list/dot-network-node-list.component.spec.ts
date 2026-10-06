import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNetworkNodeListComponent } from './dot-network-node-list.component';

import { createDownNode, createNode } from '../../../testing/dot-network.mocks';

const MESSAGES = {
    'network.nodes': 'Nodes',
    'network.node.this-node': 'This node',
    'network.node.contacted': 'Contacted {0}',
    'network.node.contacted.never': 'No heartbeat recorded',
    'network.time.seconds-ago': '{0} seconds ago',
    'network.time.minutes-ago': '{0} minutes ago',
    'network.status.up': 'Up',
    'network.status.down': 'Down'
};

describe('DotNetworkNodeListComponent', () => {
    let spectator: Spectator<DotNetworkNodeListComponent>;

    const NODES = [createNode(), createDownNode()];

    const createComponent = createComponentFactory({
        component: DotNetworkNodeListComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }],
        detectChanges: false
    });

    beforeEach(async () => {
        spectator = createComponent();
        spectator.setInput('nodes', NODES);
        spectator.setInput('currentServerId', NODES[0].serverId);
        spectator.setInput('selectedServerId', NODES[1].serverId);
        // `ngModel` hands its value to the listbox asynchronously.
        await spectator.fixture.whenStable();
        spectator.detectChanges();
    });

    it('should render one card per node with its identity and last contact', () => {
        const cards = spectator.queryAll(byTestId('network-node-card'));

        expect(cards).toHaveLength(2);
        expect(
            spectator.queryAll(byTestId('network-node-id')).map((el) => el.textContent?.trim())
        ).toEqual(['10b10fa5', 'e93aa517']);
        expect(spectator.queryAll(byTestId('network-node-name'))[0]).toHaveText(
            'dotcms-demo-56cc78659b-kgzws'
        );
        expect(spectator.queryAll(byTestId('network-node-contacted'))[0]).toHaveText(
            'Contacted 4 seconds ago'
        );
        expect(spectator.queryAll(byTestId('network-node-contacted'))[1]).toHaveText(
            'Contacted 12 minutes ago'
        );
    });

    it('should show the node count', () => {
        expect(spectator.query(byTestId('network-node-count'))).toHaveText('2');
    });

    it('should mark only the node serving the request as "This node"', () => {
        const markers = spectator.queryAll(byTestId('network-node-current'));

        expect(markers).toHaveLength(1);
        expect(spectator.queryAll(byTestId('network-node-card'))[0]).toContainText('This node');
    });

    it('should show each node status', () => {
        const statuses = spectator.queryAll(byTestId('network-node-status'));

        expect(statuses[0]).toHaveText('Up');
        expect(statuses[1]).toHaveText('Down');
    });

    it('should mark the selected option', () => {
        const options = spectator.queryAll('[role="option"]');

        expect(options).toHaveLength(2);
        expect(options[0].getAttribute('aria-selected')).toBe('false');
        expect(options[1].getAttribute('aria-selected')).toBe('true');
    });

    it('should emit the server id when an option is clicked', () => {
        let selected: string | undefined;
        spectator.output('selectNode').subscribe((id) => (selected = id as string));

        spectator.click(spectator.queryAll('[role="option"]')[0]);

        expect(selected).toBe(NODES[0].serverId);
    });

    it('should keep the selection when the selected option is clicked again', () => {
        let emitted = false;
        spectator.output('selectNode').subscribe(() => (emitted = true));

        spectator.click(spectator.queryAll('[role="option"]')[1]);

        expect(emitted).toBe(false);
    });

    it('should say when a node never wrote a heartbeat', () => {
        spectator.setInput('nodes', [createDownNode({ lastContactSeconds: null })]);

        expect(spectator.query(byTestId('network-node-contacted'))).toHaveText(
            'No heartbeat recorded'
        );
    });
});

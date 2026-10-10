import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNetworkSectionComponent } from './dot-network-section.component';

const MESSAGES = {
    'network.assets.title': 'Assets',
    'network.assets.read': 'Read',
    'network.assets.shared-path': 'Shared path',
    'network.health.green': 'Healthy',
    'network.health.yellow': 'Degraded',
    'network.health.unknown': 'Unknown'
};

describe('DotNetworkSectionComponent', () => {
    let spectator: Spectator<DotNetworkSectionComponent>;

    const createComponent = createComponentFactory({
        component: DotNetworkSectionComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                titleKey: 'network.assets.title',
                health: 'GREEN',
                rows: [
                    { labelKey: 'network.assets.shared-path', value: '/data/assets', mono: true },
                    { labelKey: 'network.assets.read', value: null }
                ]
            }
        });
    });

    it('should render the title and the health tag', () => {
        expect(spectator.query(byTestId('network-section-title'))).toHaveText('Assets');
        expect(spectator.query(byTestId('network-section-health'))).toHaveText('Healthy');
    });

    it.each([
        ['YELLOW', 'Degraded'],
        ['UNKNOWN', 'Unknown']
    ] as const)('should label %s health as %s', (health, label) => {
        spectator.setInput('health', health);

        expect(spectator.query(byTestId('network-section-health'))).toHaveText(label);
    });

    it('should render every row and an em dash for a missing value', () => {
        const rows = spectator.queryAll(byTestId('network-section-row'));
        const values = spectator.queryAll(byTestId('network-section-value'));

        expect(rows).toHaveLength(2);
        expect(rows[0]).toContainText('Shared path');
        expect(values[0]).toHaveText('/data/assets');
        expect(values[0]).toHaveClass('font-mono');
        expect(values[1]).toHaveText('—');
    });

    it('should render the subtitle only when set', () => {
        expect(spectator.query(byTestId('network-section-subtitle'))).toBeNull();

        spectator.setInput('subtitle', 'Shared volume');

        expect(spectator.query(byTestId('network-section-subtitle'))).toHaveText('Shared volume');
    });
});

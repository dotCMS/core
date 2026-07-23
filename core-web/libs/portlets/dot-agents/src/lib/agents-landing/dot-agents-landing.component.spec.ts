import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { provideRouter } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAgentsLandingComponent } from './dot-agents-landing.component';

import { DOT_AGENTS } from '../agent-registry';

describe('DotAgentsLandingComponent', () => {
    let spectator: Spectator<DotAgentsLandingComponent>;

    const createComponent = createComponentFactory({
        component: DotAgentsLandingComponent,
        providers: [
            provideRouter([]),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'agents.landing.title': 'AI Agents',
                    'agents.status.coming-soon': 'Coming soon',
                    'agents.a11y.label': 'Accessibility Studio'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('renders a card per registered agent', () => {
        const cards = DOT_AGENTS.map((agent) => spectator.query(byTestId(`agent-card-${agent.id}`)));
        expect(cards.every(Boolean)).toBe(true);
        expect(cards.length).toBe(DOT_AGENTS.length);
    });

    it('links available agents to their id and skips the link on coming-soon', () => {
        for (const agent of DOT_AGENTS) {
            const card = spectator.query(byTestId(`agent-card-${agent.id}`));

            if (agent.status === 'available') {
                expect(card?.tagName.toLowerCase()).toBe('a');
                expect(card?.getAttribute('href')).toContain(agent.id);
            } else {
                expect(card?.getAttribute('aria-disabled')).toBe('true');
            }
        }
    });

    it('shows a coming-soon tag only on unavailable agents', () => {
        const comingSoon = DOT_AGENTS.filter((a) => a.status === 'coming-soon');
        const tags = spectator.queryAll('p-tag');
        expect(tags.length).toBe(comingSoon.length);
    });
});

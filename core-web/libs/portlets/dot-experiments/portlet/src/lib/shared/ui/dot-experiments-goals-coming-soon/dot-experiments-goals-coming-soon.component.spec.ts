import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Card, CardModule } from 'primeng/card';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsGoalsComingSoonComponent } from './dot-experiments-goals-coming-soon.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.coming.soon': 'Coming soon',
    'experiments.configure.coming.soon.time.page': 'Time on page or site',
    'experiments.configure.coming.soon.time.page.description':
        'The time the user remains on the page or site',
    'experiments.configure.coming.soon.number.pages': 'Number of Pages',
    'experiments.configure.coming.soon.number.pages.description':
        'The number of pages the user visits on the site',
    'experiments.configure.coming.soon.rule.based': 'Rule-based',
    'experiments.configure.coming.soon.rule.based.description':
        'User-defined conversion conditions using dotCMS Rules',
    'experiments.configure.coming.soon.javascript': 'Javascript-triggered',
    'experiments.configure.coming.soon.javascript.description':
        'User-defined conversion conditions using Javascript to call an API'
});

fdescribe('DotExperimentsGoalsComingSoonComponent', () => {
    let spectator: Spectator<DotExperimentsGoalsComingSoonComponent>;

    const createComponent = createComponentFactory({
        imports: [CardModule],
        component: DotExperimentsGoalsComingSoonComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent();
    });

    it('should render content', () => {
        const cards: Card[] = spectator.queryAll(Card);

        expect(cards[0].header).toBe('Time on page or site');
        expect(cards[0].contentTemplate.elementRef).toContainText(
            'The time the user remains on the page or site'
        );
    });
});

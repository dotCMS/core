import { byTestId, createHostFactory, SpectatorHost } from '@openng/spectator/vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationCardComponent } from './dot-configuration-card.component';

describe('DotConfigurationCardComponent', () => {
    let spectator: SpectatorHost<DotConfigurationCardComponent>;

    const createHost = createHostFactory({
        component: DotConfigurationCardComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.locale.title': 'Locale',
                    'configuration.locale.subtitle': 'Language and time zone'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dot-configuration-card icon="language" titleKey="configuration.locale.title" subtitleKey="configuration.locale.subtitle">
                <span card-header-end data-testid="header-end">14:20 UTC</span>
                <p data-testid="body">Body</p>
            </dot-configuration-card>`
        );
    });

    it('renders the icon, title and subtitle in the header', () => {
        expect(spectator.query(byTestId('configuration-card-icon'))).toHaveText('language');
        expect(spectator.query(byTestId('configuration-card-title'))).toHaveText('Locale');
        expect(spectator.query(byTestId('configuration-card-subtitle'))).toHaveText(
            'Language and time zone'
        );
    });

    const slotOf = (testId: string) =>
        spectator.query(byTestId(testId))?.closest('[data-testid^="configuration-card-"]');

    it('projects header-end content into the header, outside the body', () => {
        expect(spectator.query(byTestId('header-end'))).toHaveText('14:20 UTC');
        expect(slotOf('header-end')).toHaveAttribute('data-testid', 'configuration-card-header');
    });

    it('projects everything else into the form body', () => {
        expect(spectator.query(byTestId('body'))).toHaveText('Body');
        expect(slotOf('body')).toHaveAttribute('data-testid', 'configuration-card-body');
    });
});

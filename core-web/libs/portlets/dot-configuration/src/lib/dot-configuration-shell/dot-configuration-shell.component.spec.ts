import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { DotConfigurationShellComponent } from './dot-configuration-shell.component';

describe('DotConfigurationShellComponent', () => {
    let spectator: Spectator<DotConfigurationShellComponent>;

    const createComponent = createComponentFactory({
        component: DotConfigurationShellComponent,
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('renders the scrollable page container', () => {
        expect(spectator.query(byTestId('configuration-page'))).toExist();
    });

    it('renders the sections column inside the page', () => {
        expect(
            spectator
                .query(byTestId('configuration-page'))
                ?.querySelector('[data-testid="configuration-sections"]')
        ).toExist();
    });
});

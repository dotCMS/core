import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { DotExperimentsConfigurationItemsCountComponent } from './dot-experiments-configuration-items-count.component';

describe('DotExperimentsConfigurationItemsCountComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationItemsCountComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurationItemsCountComponent
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
    });

    it('should create', () => {
        const maxLength = 5;
        const count = 1;

        spectator.setInput({
            maxLength,
            count
        });
        expect(spectator.query(byTestId('count-text'))).toContainText(`${count}/${maxLength}`);
    });
});

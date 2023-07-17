import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent } from '@dotcms/ui';

import { DotExperimentsAnalyticAppMisconfigurationComponent } from './dot-experiments-analytic-app-misconfiguration.component';

describe('DotExperimentsAnalyticAppMisconfigurationComponent', () => {
    let spectator: Spectator<DotExperimentsAnalyticAppMisconfigurationComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsAnalyticAppMisconfigurationComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should DotEmptyContainerComponent exist in the component', () => {
        expect(spectator.query(DotEmptyContainerComponent)).toExist();
    });
});

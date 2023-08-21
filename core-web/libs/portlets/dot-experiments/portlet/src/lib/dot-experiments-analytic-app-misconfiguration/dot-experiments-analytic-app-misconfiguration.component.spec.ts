import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';

import { Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent } from '@dotcms/ui';

import { DotExperimentsAnalyticAppMisconfigurationComponent } from './dot-experiments-analytic-app-misconfiguration.component';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

describe('DotExperimentsAnalyticAppMisconfigurationComponent', () => {
    let spectator: Spectator<DotExperimentsAnalyticAppMisconfigurationComponent>;
    let router: SpyObject<Router>;
    const createComponent = createComponentFactory({
        component: DotExperimentsAnalyticAppMisconfigurationComponent,
        providers: [mockProvider(DotMessageService), mockProvider(Router)]
    });

    beforeEach(() => {
        spectator = createComponent();

        router = spectator.inject(Router);
    });

    it('should DotEmptyContainerComponent exist in the component', () => {
        expect(spectator.query(DotEmptyContainerComponent)).toExist();

        spectator.query(DotEmptyContainerComponent).buttonAction.emit();

        expect(router.navigate).toHaveBeenCalledWith(['/apps/dotAnalytics-config']);
    });

    it('should DotExperimentsUiHeaderComponent exist in the component', () => {
        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();

        spectator.query(DotExperimentsUiHeaderComponent).goBack.emit();

        expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
            queryParamsHandling: 'merge'
        });
    });
});

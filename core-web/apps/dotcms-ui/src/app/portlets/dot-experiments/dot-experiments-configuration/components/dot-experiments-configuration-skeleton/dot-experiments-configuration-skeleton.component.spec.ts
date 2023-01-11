import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { DotExperimentsConfigurationSkeletonComponent } from './dot-experiments-configuration-skeleton.component';

describe('DotExperimentsConfigurationSkeletonComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationSkeletonComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurationSkeletonComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});

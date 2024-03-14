import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { DotEmaRunningExperimentComponent } from './dot-ema-running-experiment.component';

describe('DotEmaRunningExperimentComponent', () => {
    let spectator: Spectator<DotEmaRunningExperimentComponent>;
    const createComponent = createComponentFactory(DotEmaRunningExperimentComponent);

    // TEST MISSING
    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});

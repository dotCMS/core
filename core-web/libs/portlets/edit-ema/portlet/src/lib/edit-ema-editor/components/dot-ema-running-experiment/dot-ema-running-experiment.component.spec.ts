import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService } from '@dotcms/data-access';
import { getRunningExperimentMock } from '@dotcms/utils-testing';

import { DotEmaRunningExperimentComponent } from './dot-ema-running-experiment.component';

describe('DotEmaRunningExperimentComponent', () => {
    let spectator: Spectator<DotEmaRunningExperimentComponent>;
    const runningExperiment = getRunningExperimentMock();
    const createComponent = createComponentFactory({
        component: DotEmaRunningExperimentComponent,
        declarations: [],
        imports: [RouterTestingModule],
        providers: [
            mockProvider(ActivatedRoute),
            {
                provide: DotMessageService,
                useValue: {
                    get: (key: string) => key
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                runningExperiment: runningExperiment
            }
        });
    });
    it('should render a tag', () => {
        expect(spectator.component).toBeTruthy();

        const tag = spectator.query(byTestId('runningExperimentTag'));

        expect(tag).toBeTruthy();
    });

    it('should have a tag with router link', () => {
        const tag = spectator.query(byTestId('runningExperimentTag'));

        expect(tag.getAttribute('ng-reflect-router-link')).toBeTruthy();
    });
});

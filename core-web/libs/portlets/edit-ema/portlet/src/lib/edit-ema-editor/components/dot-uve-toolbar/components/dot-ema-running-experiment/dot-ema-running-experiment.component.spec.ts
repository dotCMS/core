import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
        // In Angular 20, ng-reflect-* attributes are not available
        // Verify the routerLink directive is present and configured
        const tagDebugElement = spectator.debugElement.query(
            By.css('[data-testId="runningExperimentTag"]')
        );
        const routerLinkDirective = tagDebugElement?.injector.get(RouterLink, null);
        expect(routerLinkDirective).toBeTruthy();
        // Verify that the tag element exists and has routerLink directive applied
        // The routerLink directive is applied to the p-tag element
        expect(tagDebugElement).toBeTruthy();
    });
});

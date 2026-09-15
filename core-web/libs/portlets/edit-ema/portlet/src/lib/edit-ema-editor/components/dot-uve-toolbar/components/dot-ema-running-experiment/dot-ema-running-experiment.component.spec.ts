import {
    Spectator,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/vitest';

import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperiment } from '@dotcms/dotcms-models';
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
        // `setInput` with the public alias, not `props`: the input is declared as
        // `$runningExperiment` with `alias: 'runningExperiment'`, and Spectator's typed `props`
        // keys off the field name without mapping the alias back — it type-checks and then leaves
        // a required input unset (NG0950).
        spectator = createComponent({ detectChanges: false });
        spectator.setInput('runningExperiment', runningExperiment);
        spectator.detectChanges();
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
            By.css('[data-testid="runningExperimentTag"]')
        );
        const routerLinkDirective = tagDebugElement?.injector.get(RouterLink, null);
        expect(routerLinkDirective).toBeTruthy();
        // Verify that the tag element exists and has routerLink directive applied
        // The routerLink directive is applied to the p-tag element
        expect(tagDebugElement).toBeTruthy();
    });

    /**
     * FR-017 (#37005): the reports destination must be unchanged by the entry-point switch work.
     *
     * The tag is deliberately NOT switched. The new portlet has no `:id/results` route —
     * `lib.routes.ts` omits it pending #37004, "so the router surfaces an honest 404 instead of
     * falling back to the legacy UVE screens" — so pointing an opted-in operator at it would route
     * them into a 404. FR-017 constrains only the switch-off case and is satisfied by leaving the
     * tag alone; Section D governs the navigation *item*, not the tag.
     *
     * The existing test above asserts a routerLink is *present*. This one asserts *where it goes*,
     * which is the part that would regress silently.
     */
    it('should link to the legacy reports route, keyed on page and experiment', () => {
        const tagDebugElement = spectator.debugElement.query(
            By.css('[data-testid="runningExperimentTag"]')
        );
        const routerLink = tagDebugElement?.injector.get(RouterLink, null);

        // `RouterLink.routerLink` is a setter and reads back undefined, so the destination is
        // asserted through the URL the directive actually resolves.
        const href = spectator.inject(Router).serializeUrl(routerLink.urlTree);

        expect(href).toBe(
            `/edit-page/experiments/${runningExperiment.pageId}/${runningExperiment.id}/reports`
        );
    });
    /**
     * #37478, FR-025c, D14. Being told which experiment is running must not cost the editor the
     * page they are being told about. As an action the tag drops its destination entirely — a
     * `routerLink` left in place would navigate on the same click that opens the panel.
     */
    describe('as an action (#37478)', () => {
        beforeEach(() => {
            spectator = createComponent({ detectChanges: false });
            spectator.setInput('runningExperiment', runningExperiment);
            spectator.setInput('asAction', true);
            spectator.detectChanges();
        });

        it('should carry no destination', () => {
            const tag = spectator.debugElement.query(
                By.css('[data-testid="runningExperimentTag"]')
            );

            expect(tag?.injector.get(RouterLink, null)?.urlTree).toBeFalsy();
        });

        it('should report the experiment it is about', () => {
            const seen: DotExperiment[] = [];
            spectator.output<DotExperiment>('viewResults').subscribe((e) => seen.push(e));

            spectator.click(byTestId('runningExperimentTag'));

            expect(seen).toEqual([runningExperiment]);
        });

        it('should stay a destination when it is not an action', () => {
            spectator.setInput('asAction', false);
            spectator.detectChanges();

            const tag = spectator.debugElement.query(
                By.css('[data-testid="runningExperimentTag"]')
            );
            const href = spectator
                .inject(Router)
                .serializeUrl(tag.injector.get(RouterLink).urlTree);

            expect(href).toBe(
                `/edit-page/experiments/${runningExperiment.pageId}/${runningExperiment.id}/reports`
            );
        });
    });
});

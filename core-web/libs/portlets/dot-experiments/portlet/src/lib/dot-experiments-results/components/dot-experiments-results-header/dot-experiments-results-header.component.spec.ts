import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { MockInstance, vi } from 'vitest';

import { provideLocationMocks } from '@angular/common/testing';
import { ActivatedRoute, Params, provideRouter, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperiment, DotExperimentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsResultsHeaderComponent } from './dot-experiments-results-header.component';

import { DotExperimentsResultsStore } from '../../../store/dot-experiments-results.store';

const EXPERIMENT = {
    id: '1111-1111-1111-1111',
    name: 'Summer landing test',
    status: DotExperimentStatus.RUNNING,
    trafficProportion: { type: 'SPLIT_EVENLY', variants: [] }
} as unknown as DotExperiment;

const messageServiceMock = new MockDotMessageService({
    'experiments.results.action.configuration': 'Configuration',
    'experiments.action.end-experiment': 'Stop Experiment',
    'experiments.results.header.variants': '{0} Variants',
    'experiments.configure.header.no-page': 'No Page selected'
});

const createStoreMock = () => ({
    experiment: vi.fn().mockReturnValue(EXPERIMENT),
    page: vi.fn().mockReturnValue(null),
    $status: vi.fn().mockReturnValue(DotExperimentStatus.RUNNING),
    $isSaving: vi.fn().mockReturnValue(false)
});

/**
 * The two ways out of the Results screen (#37005).
 *
 * Both read the page narrowing off the address rather than holding it, for the reason the Configure
 * header's back button does: this screen can be reached from a list narrowed to one page, and a
 * departure that dropped `pageId` landed the editor on every experiment on the site — a list they
 * had not asked for. `onConfiguration` was the last of the three to still drop it.
 */
describe('DotExperimentsResultsHeaderComponent', () => {
    let spectator: Spectator<DotExperimentsResultsHeaderComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    /** The address the screen arrived on, as `ActivatedRoute` reports it. */
    let routeQueryParams: Params;
    let navigate: MockInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsResultsHeaderComponent,
        providers: [
            provideRouter([{ path: 'experiments', children: [] }]),
            provideLocationMocks(),
            // Overrides the one `provideRouter` installs: these tests set the arrival address
            // directly rather than driving a navigation to it.
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        get queryParams() {
                            return routeQueryParams;
                        }
                    }
                }
            },
            { provide: DotExperimentsResultsStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            ConfirmationService
        ],
        detectChanges: false
    });

    const clickButton = (testId: string) => {
        const host = spectator.query(byTestId(testId));
        spectator.click(host?.querySelector('button') as HTMLElement);
        spectator.detectChanges();
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        routeQueryParams = {};
        spectator = createComponent();
        navigate = vi.spyOn(spectator.inject(Router), 'navigate').mockResolvedValue(true);
        spectator.detectChanges();
    });

    describe('back to the list', () => {
        it('should return to the site-wide list when the address carried no narrowing', () => {
            clickButton('experiments-results-back-btn');

            expect(navigate).toHaveBeenCalledWith(['/experiments'], { queryParams: {} });
        });

        it('should return to the list still narrowed to the page it arrived with', () => {
            routeQueryParams = { pageId: 'page-1', language_id: '2' };
            spectator.detectChanges();

            clickButton('experiments-results-back-btn');

            expect(navigate).toHaveBeenCalledWith(['/experiments'], {
                queryParams: { pageId: 'page-1', language_id: '2' }
            });
        });
    });

    describe('to the Configure screen', () => {
        it('should open the experiment being reported on', () => {
            clickButton('experiments-results-configuration-btn');

            expect(navigate).toHaveBeenCalledWith(
                ['/experiments', EXPERIMENT.id, 'configuration'],
                { queryParams: {} }
            );
        });

        // Configure has a back button of its own, reading the same address: arriving there without
        // `pageId` left it pointing at the site-wide list, so Results → Configuration → Back
        // widened a narrowing the editor never cleared.
        it('should carry the page narrowing across', () => {
            routeQueryParams = { pageId: 'page-1', language_id: '2' };
            spectator.detectChanges();

            clickButton('experiments-results-configuration-btn');

            expect(navigate).toHaveBeenCalledWith(
                ['/experiments', EXPERIMENT.id, 'configuration'],
                { queryParams: { pageId: 'page-1', language_id: '2' } }
            );
        });
    });
});

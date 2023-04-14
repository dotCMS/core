import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, DotExperimentStatusList } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsPublishVariantComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-publish-variant/dot-experiments-publish-variant.component';
import { DotExperimentsReportsSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-skeleton/dot-experiments-reports-skeleton.component';
import {
    DotExperimentsReportsStore,
    VmReportExperiment
} from '@portlets/dot-experiments/dot-experiments-reports/store/dot-experiments-reports-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotExperimentsExperimentSummaryComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import {
    DotExperimentsReportsStoreMock,
    getExperimentMock,
    getExperimentResultsMock
} from '@portlets/dot-experiments/test/mocks';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsReportsComponent } from './dot-experiments-reports.component';

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: '1111'
        }
    }
};

const defaultVmMock: VmReportExperiment = {
    experiment: getExperimentMock(3),
    results: getExperimentResultsMock(1),
    variantResults: null,
    chartResults: null,
    isLoading: false,
    showSummary: false,
    status: ComponentStatus.INIT
};

const EXPERIMENT_MOCK = getExperimentMock(0);

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'xx'
});

describe('DotExperimentsReportsComponent', () => {
    let spectator: Spectator<DotExperimentsReportsComponent>;
    let router: SpyObject<Router>;
    let store: DotExperimentsReportsStore;

    const createComponent = createComponentFactory({
        imports: [
            DotExperimentsUiHeaderComponent,
            DotExperimentsReportsSkeletonComponent,
            DotExperimentsExperimentSummaryComponent,
            DotExperimentsPublishVariantComponent,
            DotDynamicDirective
        ],
        component: DotExperimentsReportsComponent,
        componentProviders: [
            mockProvider(DotExperimentsReportsStore, DotExperimentsReportsStoreMock)
        ],
        providers: [
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(Router),
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        store = spectator.inject(DotExperimentsReportsStore, true);
        router = spectator.inject(Router);
    });

    it('should show the skeleton component when is loading', () => {
        spectator.component.vm$ = of({ ...defaultVmMock, isLoading: true });
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsReportsSkeletonComponent)).toExist();
    });

    it("shouldn't show the skeleton component when is not loading", () => {
        spectator.component.vm$ = of({ ...defaultVmMock, isLoading: false });
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsReportsSkeletonComponent)).not.toExist();
    });

    it('should show the SummaryComponent', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            experiment: {
                ...defaultVmMock.experiment,
                status: DotExperimentStatusList.RUNNING
            },
            isLoading: false,
            showSummary: true
        });
        spectator.detectChanges();
        expect(spectator.query(DotExperimentsExperimentSummaryComponent)).toExist();
    });

    it('should back to Experiment List', () => {
        spectator.detectComponentChanges();
        spectator.component.goToExperimentList(EXPERIMENT_MOCK.pageId);
        expect(router.navigate).toHaveBeenCalledWith(
            ['/edit-page/experiments/', EXPERIMENT_MOCK.pageId],
            {
                queryParams: {
                    mode: null,
                    variantName: null,
                    experimentId: null
                },
                queryParamsHandling: 'merge'
            }
        );
    });

    it('should load the publish variant dialog and promote the variant.', () => {
        spyOn(store, 'promoteVariant');
        spectator.component.vm$ = of({ ...defaultVmMock, isLoading: false });
        spectator.detectChanges();

        spectator.click(byTestId('publish-variant-button'));

        const dialog = spectator.query(DotExperimentsPublishVariantComponent);

        dialog.publish.emit('1');

        expect(store.promoteVariant).toHaveBeenCalledWith('1');
        expect(spectator.query(DotExperimentsPublishVariantComponent)).not.toExist();
    });

    it('should load the publish variant dialog and close', () => {
        spectator.component.vm$ = of({ ...defaultVmMock, isLoading: false });
        spectator.detectChanges();
        spectator.click(byTestId('publish-variant-button'));

        const dialog = spectator.query(DotExperimentsPublishVariantComponent);

        dialog.hide.emit('');
        expect(spectator.query(DotExperimentsPublishVariantComponent)).not.toExist();
    });
});

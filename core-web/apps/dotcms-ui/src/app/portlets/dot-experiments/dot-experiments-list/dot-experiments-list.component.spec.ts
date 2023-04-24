import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsCreateComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-create/dot-experiments-create.component';
import { DotExperimentsEmptyExperimentsComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import { DotExperimentsListSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { DotExperimentsListTableComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-table/dot-experiments-list-table.component';
import { DotExperimentsStatusFilterComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import {
    DotExperimentsListStore,
    VmListExperiments
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import {
    ActivatedRouteListStoreMock,
    getExperimentAllMocks,
    getExperimentMock
} from '@portlets/dot-experiments/test/mocks';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsListComponent } from './dot-experiments-list.component';

const EXPERIMENT_MOCKS = getExperimentAllMocks();

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('ExperimentsListComponent', () => {
    let spectator: Spectator<DotExperimentsListComponent>;
    let dotExperimentsStatusFilterComponent: DotExperimentsStatusFilterComponent;
    let dotExperimentsEmptyExperimentsComponent: DotExperimentsEmptyExperimentsComponent;
    let dotExperimentsListSkeletonComponent: DotExperimentsListSkeletonComponent;

    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let router: SpyObject<Router>;

    const createComponent = createComponentFactory({
        imports: [
            DotMessagePipeModule,
            SkeletonModule,
            DotIconModule,
            UiDotIconButtonTooltipModule,
            UiDotIconButtonModule,
            DotExperimentsCreateComponent,
            DotDynamicDirective,
            ButtonModule
        ],
        component: DotExperimentsListComponent,
        componentProviders: [DotExperimentsListStore],
        declarations: [
            DotExperimentsStatusFilterComponent,
            DotExperimentsListSkeletonComponent,
            DotExperimentsEmptyExperimentsComponent,
            DotExperimentsListTableComponent
        ],
        providers: [
            mockProvider(Router),
            mockProvider(DotMessagePipe),
            mockProvider(DotMessageService),
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(ConfirmationService),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: ActivatedRoute,
                useClass: ActivatedRouteListStoreMock
            }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        router = spectator.inject(Router);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getAll.and.returnValue(of(EXPERIMENT_MOCKS));
    });

    it('should show the skeleton component when is loading', () => {
        const vmListExperimentsMock$: VmListExperiments = {
            page: {
                pageId: '',
                pageTitle: ''
            },
            experiments: [],
            filterStatus: [],
            experimentsFiltered: [],
            isLoading: true,
            sidebar: {
                status: ComponentStatus.IDLE,
                isOpen: false
            }
        };
        spectator.component.vm$ = of(vmListExperimentsMock$);
        spectator.detectComponentChanges();

        dotExperimentsListSkeletonComponent = spectator.query(DotExperimentsListSkeletonComponent);
        expect(dotExperimentsListSkeletonComponent).toExist();
    });

    it('should show the empty component when is not loading and no experiments', () => {
        const vmListExperimentsMock$: VmListExperiments = {
            page: {
                pageId: '',
                pageTitle: ''
            },
            experiments: [],
            filterStatus: [],
            experimentsFiltered: [],
            isLoading: false,
            sidebar: {
                status: ComponentStatus.IDLE,
                isOpen: false
            }
        };
        spectator.component.vm$ = of(vmListExperimentsMock$);
        spectator.detectComponentChanges();

        dotExperimentsEmptyExperimentsComponent = spectator.query(
            DotExperimentsEmptyExperimentsComponent
        );
        expect(dotExperimentsEmptyExperimentsComponent).toExist();
    });

    it('should show the filters component and add experiment button exist when has experiments', () => {
        const vmListExperimentsMock$: VmListExperiments = {
            page: {
                pageId: '1111',
                pageTitle: 'title'
            },
            experiments: getExperimentAllMocks(),
            filterStatus: [],
            experimentsFiltered: [],
            isLoading: false,
            sidebar: {
                status: ComponentStatus.IDLE,
                isOpen: false
            }
        };
        spectator.component.vm$ = of(vmListExperimentsMock$);
        spectator.detectComponentChanges();

        dotExperimentsStatusFilterComponent = spectator.query(DotExperimentsStatusFilterComponent);
        expect(dotExperimentsStatusFilterComponent).toExist();

        const addExperimentButton = spectator.query(byTestId('add-experiment-button'));
        expect(addExperimentButton).toExist();
    });

    it('should show the sidebar when click ADD EXPERIMENT', () => {
        spectator.component.addExperiment();
        spectator.detectComponentChanges();

        expect(spectator.query(DotExperimentsCreateComponent)).toExist();
    });

    it('should go to Report Container', () => {
        spectator.detectComponentChanges();
        spectator.component.goToViewExperimentReport(EXPERIMENT_MOCK);
        expect(router.navigate).toHaveBeenCalledWith(
            ['/edit-page/experiments/reports/', EXPERIMENT_MOCK.id],
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
});

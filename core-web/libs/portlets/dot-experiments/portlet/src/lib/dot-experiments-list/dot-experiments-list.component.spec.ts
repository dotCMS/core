import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { provideComponentStore } from '@ngrx/component-store';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotExperimentsService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus, DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotAddToBundleComponent, DotEmptyContainerComponent } from '@dotcms/ui';
import {
    ActivatedRouteListStoreMock,
    DotExperimentsStoreMock,
    getExperimentAllMocks,
    getExperimentMock
} from '@dotcms/utils-testing';

import { DotExperimentsCreateComponent } from './components/dot-experiments-create/dot-experiments-create.component';
import { DotExperimentsListSkeletonComponent } from './components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { DotExperimentsStatusFilterComponent } from './components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotExperimentsListStore, VmListExperiments } from './store/dot-experiments-list-store';

import { DotExperimentsStore } from '../dot-experiments-shell/store/dot-experiments.store';

const EXPERIMENT_MOCKS = getExperimentAllMocks();

const EXPERIMENT_MOCK = getExperimentMock(0);

const VM_LIST_EXPERIMENTS_MOCKS$: VmListExperiments = {
    pageId: '',
    pageTitle: '',
    experiments: [],
    filterStatus: [],
    experimentsFiltered: [],
    isLoading: true,
    sidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false
    },
    addToBundleContentId: null
};

describe('ExperimentsListComponent', () => {
    let spectator: Spectator<DotExperimentsListComponent>;
    let router: SpyObject<Router>;

    const createComponent = createComponentFactory({
        component: DotExperimentsListComponent,
        componentProviders: [provideComponentStore(DotExperimentsListStore)],
        imports: [MockComponent(DotAddToBundleComponent)],
        providers: [
            ConfirmationService,
            mockProvider(DotExperimentsStore, DotExperimentsStoreMock),
            mockProvider(Router),
            mockProvider(DotMessageService),
            mockProvider(DotFormatDateService),
            mockProvider(DotExperimentsService, {
                getAll: () => of(EXPERIMENT_MOCKS)
            }),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: ActivatedRoute,
                useClass: ActivatedRouteListStoreMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        router = spectator.inject(Router);
    });

    it('should show the skeleton component when is loading', () => {
        spectator.component.vm$ = of({ ...VM_LIST_EXPERIMENTS_MOCKS$ });
        spectator.detectComponentChanges();

        expect(spectator.query(DotExperimentsListSkeletonComponent)).toExist();
    });

    it('should show the empty component when is not loading and no experiments', () => {
        spectator.component.vm$ = of({
            ...VM_LIST_EXPERIMENTS_MOCKS$,
            isLoading: false
        });
        spectator.detectComponentChanges();

        expect(spectator.query(DotEmptyContainerComponent)).toExist();
    });

    it('should show the filters component and add experiment button exist when has experiments', () => {
        spectator.component.vm$ = of({
            ...VM_LIST_EXPERIMENTS_MOCKS$,
            experiments: getExperimentAllMocks(),
            isLoading: false
        });
        spectator.detectComponentChanges();

        expect(spectator.query(DotExperimentsStatusFilterComponent)).toExist();

        const addExperimentButton = spectator.query(byTestId('add-experiment-button'));
        expect(addExperimentButton).toExist();
    });

    it('should show the sidebar when click ADD EXPERIMENT', () => {
        spectator.detectChanges();
        spectator.component.addExperiment();
        spectator.detectComponentChanges();

        expect(spectator.query(DotExperimentsCreateComponent)).toExist();
    });

    it('should go to report Container if the experiment status is RUNNING', () => {
        spectator.detectComponentChanges();
        spectator.component.goToContainerAction({
            ...EXPERIMENT_MOCK,
            status: DotExperimentStatus.RUNNING
        });
        expect(router.navigate).toHaveBeenCalledWith(
            ['/edit-page/experiments/', EXPERIMENT_MOCK.pageId, EXPERIMENT_MOCK.id, 'reports'],
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

    it('should go to report Container if the experiment status is ENDED', () => {
        spectator.detectComponentChanges();
        spectator.component.goToContainerAction({
            ...EXPERIMENT_MOCK,
            status: DotExperimentStatus.ENDED
        });
        expect(router.navigate).toHaveBeenCalledWith(
            ['/edit-page/experiments/', EXPERIMENT_MOCK.pageId, EXPERIMENT_MOCK.id, 'reports'],
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

    it('should go to configuration Container if the experiment status is DRAFT', () => {
        spectator.detectComponentChanges();
        spectator.component.goToContainerAction({
            ...EXPERIMENT_MOCK,
            status: DotExperimentStatus.DRAFT
        });
        expect(router.navigate).toHaveBeenCalledWith(
            [
                '/edit-page/experiments/',
                EXPERIMENT_MOCK.pageId,
                EXPERIMENT_MOCK.id,
                'configuration'
            ],
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

    it('should go to configuration Container if the experiment status is SCHEDULED', () => {
        spectator.detectComponentChanges();
        spectator.component.goToContainerAction({
            ...EXPERIMENT_MOCK,
            status: DotExperimentStatus.SCHEDULED
        });
        expect(router.navigate).toHaveBeenCalledWith(
            [
                '/edit-page/experiments/',
                EXPERIMENT_MOCK.pageId,
                EXPERIMENT_MOCK.id,
                'configuration'
            ],
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

    it('should show and remove add to bundle dialog', () => {
        spectator.component.vm$ = of({
            ...VM_LIST_EXPERIMENTS_MOCKS$,
            experiments: getExperimentAllMocks(),
            isLoading: false,
            addToBundleContentId: '123'
        });
        spectator.detectComponentChanges();

        const addToBundle = spectator.query(DotAddToBundleComponent);

        expect(addToBundle.assetIdentifier).toEqual('123');

        addToBundle.cancel.emit(true);

        spectator.detectComponentChanges();

        expect(spectator.query(DotAddToBundleComponent)).not.toExist();
    });
});

import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { DotExperimentsStatusFilterComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import { DotExperimentsEmptyExperimentsComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import { DotExperimentsListSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { of } from 'rxjs';
import { SkeletonModule } from 'primeng/skeleton';
import { DotExperimentsListTableComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-table/dot-experiments-list-table.component';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DotIconModule } from '@dotcms/ui';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import {
    DotExperimentsListStore,
    VmListExperiments
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store.service';
import {
    ActivatedRouteListStoreMock,
    DotExperimentsListStoreMock,
    ExperimentMocks
} from '@portlets/dot-experiments/test/mocks';
import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentsCreateComponent } from '@portlets/dot-experiments/dot-experiments-create/dot-experiments-create.component';
import { ButtonModule } from 'primeng/button';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

describe('ExperimentsListComponent', () => {
    let spectator: Spectator<DotExperimentsListComponent>;
    let dotExperimentsStatusFilterComponent: DotExperimentsStatusFilterComponent;
    let dotExperimentsEmptyExperimentsComponent: DotExperimentsEmptyExperimentsComponent;
    let dotExperimentsListSkeletonComponent: DotExperimentsListSkeletonComponent;

    const createComponent = createComponentFactory({
        imports: [
            DotMessagePipeModule,
            SkeletonModule,
            DotIconModule,
            UiDotIconButtonTooltipModule,
            UiDotIconButtonModule,
            DotExperimentsCreateComponent,
            ButtonModule
        ],
        component: DotExperimentsListComponent,
        componentProviders: [
            { provide: DotExperimentsListStore, useValue: DotExperimentsListStoreMock },
            mockProvider(ConfirmationService),
            mockProvider(MessageService),
            mockProvider(DotMessageService)
        ],
        declarations: [
            DotExperimentsStatusFilterComponent,
            DotExperimentsListSkeletonComponent,
            DotExperimentsEmptyExperimentsComponent,
            DotExperimentsListTableComponent
        ],
        providers: [
            mockProvider(DotMessagePipe),
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
    });

    it('should show the skeleton component when is loading', () => {
        const vmListExperimentsMock$: VmListExperiments = {
            page: {
                pageId: '',
                pageTitle: ''
            },
            experiments: [],
            filterStatus: [],
            experimentsFiltered: {},
            isLoading: true
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
            experimentsFiltered: {},
            isLoading: false
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
            experiments: ExperimentMocks,
            filterStatus: [],
            experimentsFiltered: {},
            isLoading: false
        };
        spectator.component.vm$ = of(vmListExperimentsMock$);
        spectator.detectComponentChanges();

        dotExperimentsStatusFilterComponent = spectator.query(DotExperimentsStatusFilterComponent);
        expect(dotExperimentsStatusFilterComponent).toExist();

        const addExperimentButton = spectator.query(byTestId('add-experiment-button'));
        expect(addExperimentButton).toExist();
    });
});

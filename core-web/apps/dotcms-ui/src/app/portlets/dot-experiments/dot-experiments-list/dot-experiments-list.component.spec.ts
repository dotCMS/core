import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { DotExperimentsStore } from '@portlets/dot-experiments/shared/stores/dot-experiments-store.service';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotExperimentsStatusFilterComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import { DotExperimentsEmptyExperimentsComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import { DotExperimentsListSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { of } from 'rxjs';
import {
    DotExperiment,
    DotExperimentStatusList,
    TrafficProportionTypes
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { SkeletonModule } from 'primeng/skeleton';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { DotExperimentsListTableComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-table/dot-experiments-list-table.component';
import { ConfirmationService } from 'primeng/api';
import { DotIconModule } from '@dotcms/ui';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

class storeMock {
    loadExperiments() {
        return [];
    }
}

@Pipe({ name: 'dm' })
class MockDmPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

interface VmListExperiments {
    isLoading: boolean;
    experiments: DotExperiment[];
    experimentsFiltered: { [key: string]: DotExperiment[] };
    experimentsFilterStatus: Array<string>;
}

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
            UiDotIconButtonModule
        ],
        component: DotExperimentsListComponent,
        declarations: [
            DotExperimentsStatusFilterComponent,
            DotExperimentsListSkeletonComponent,
            DotExperimentsEmptyExperimentsComponent,
            MockDmPipe,
            DotExperimentsListTableComponent
        ],
        providers: [
            { provide: DotExperimentsStore, useClass: storeMock },
            mockProvider(ConfirmationService),
            mockProvider(DotMessageService),
            mockProvider(DotExperimentsService)
        ],
        schemas: [NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false // <-- no trigger ngOnInit
        });
    });

    it('should show the skeleton component when is loading', () => {
        const vmListExperimentsMock$: VmListExperiments = {
            experiments: [],
            experimentsFilterStatus: [],
            experimentsFiltered: {},
            isLoading: true
        };
        spectator.component.vmListExperiments$ = of(vmListExperimentsMock$);
        spectator.detectChanges();

        dotExperimentsListSkeletonComponent = spectator.query(DotExperimentsListSkeletonComponent);
        expect(dotExperimentsListSkeletonComponent).toExist();
    });

    it('should show the empty component when is not loading and no experiments', () => {
        const vmListExperimentsMock$: VmListExperiments = {
            experiments: [],
            experimentsFilterStatus: [],
            experimentsFiltered: {},
            isLoading: false
        };
        spectator.component.vmListExperiments$ = of(vmListExperimentsMock$);
        spectator.detectChanges();

        dotExperimentsEmptyExperimentsComponent = spectator.query(
            DotExperimentsEmptyExperimentsComponent
        );
        expect(dotExperimentsEmptyExperimentsComponent).toExist();
    });

    it('should show the filters component and add experiment button exist when has experiments', () => {
        const vmListExperimentsMock$: VmListExperiments = {
            experiments: [
                {
                    id: '111',
                    pageId: '456',
                    status: DotExperimentStatusList.DRAFT,
                    archived: false,
                    readyToStart: false,
                    description: 'Praesent at molestie mauris, quis vulputate augue.',
                    name: 'Praesent at molestie mauris',
                    trafficAllocation: 100.0,
                    scheduling: null,
                    trafficProportion: {
                        percentages: {},
                        type: TrafficProportionTypes.SPLIT_EVENLY
                    },
                    creationDate: new Date('2022-08-21 14:50:03'),
                    modDate: new Date('2022-08-21 18:50:03')
                }
            ],
            experimentsFilterStatus: [],
            experimentsFiltered: {},
            isLoading: false
        };
        spectator.component.vmListExperiments$ = of(vmListExperimentsMock$);
        spectator.detectChanges();

        dotExperimentsStatusFilterComponent = spectator.query(DotExperimentsStatusFilterComponent);
        expect(dotExperimentsStatusFilterComponent).toExist();

        const addExperimentButton = spectator.query(byTestId('add-experiment-button'));
        expect(addExperimentButton).toExist();
    });
});

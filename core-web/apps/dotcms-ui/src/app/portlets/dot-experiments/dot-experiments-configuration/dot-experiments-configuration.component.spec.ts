import { DotExperimentsConfigurationComponent } from './dot-experiments-configuration.component';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ButtonModule } from 'primeng/button';
import { of } from 'rxjs';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotExperimentsConfigurationExperimentStatusBarComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-experiment-status-bar/dot-experiments-configuration-experiment-status-bar.component';
import { DotExperimentsConfigurationVariantsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants/dot-experiments-configuration-variants.component';
import { DotExperimentsConfigurationTargetingComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-targeting/dot-experiments-configuration-targeting.component';
import { DotExperimentsConfigurationTrafficComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-traffic/dot-experiments-configuration-traffic.component';
import { DotExperimentsConfigurationSchedulingComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-scheduling/dot-experiments-configuration-scheduling.component';
import { DotExperimentsConfigurationSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsConfigurationGoalsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import {
    DotExperimentsConfigurationStoreMock,
    ExperimentMocks
} from '@portlets/dot-experiments/test/mocks';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessageService } from '@dotcms/data-access';
import { MessageService } from 'primeng/api';
import { DotSessionStorageService } from '@dotcms/data-access';
import { Status } from '@dotcms/dotcms-models';

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: '1111',
            pageId: '222'
        }
    }
};

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'Scheduling',
    'experiments.configure.scheduling.start': 'When the experiment start'
});

describe('DotExperimentsConfigurationComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationComponent>;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [
            HttpClientTestingModule,
            ButtonModule,
            DotExperimentsUiHeaderComponent,
            DotExperimentsConfigurationGoalsComponent,
            DotExperimentsConfigurationTargetingComponent,
            DotExperimentsConfigurationVariantsComponent,
            DotExperimentsConfigurationTrafficComponent,
            DotExperimentsConfigurationSchedulingComponent,
            DotExperimentsConfigurationSkeletonComponent,
            DotExperimentsConfigurationExperimentStatusBarComponent
        ],
        component: DotExperimentsConfigurationComponent,
        componentProviders: [
            mockProvider(DotExperimentsConfigurationStore, DotExperimentsConfigurationStoreMock)
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
            mockProvider(DotExperimentsService),
            mockProvider(DotSessionStorageService),
            mockProvider(MessageService),
            mockProvider(Router),
            mockProvider(Title)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.returnValue(of(ExperimentMocks[0]));
    });

    it('should show the skeleton component when is loading', () => {
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationSkeletonComponent)).toExist();
    });

    it('should load all the components', () => {
        const vmMock$ = {
            experiment: ExperimentMocks[0],
            stepStatusSidebar: {
                status: Status.IDLE,
                isOpen: false,
                experimentStep: null
            },
            isLoading: false
        };
        spectator.component.vm$ = of(vmMock$);
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationExperimentStatusBarComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationVariantsComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationGoalsComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationTargetingComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationTrafficComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationSchedulingComponent)).toExist();
    });
});

import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

import { DotMessageService, DotSessionStorageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationGoalsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationSchedulingComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-scheduling/dot-experiments-configuration-scheduling.component';
import { DotExperimentsConfigurationSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsConfigurationTargetingComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-targeting/dot-experiments-configuration-targeting.component';
import { DotExperimentsConfigurationTrafficComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-traffic/dot-experiments-configuration-traffic.component';
import { DotExperimentsConfigurationVariantsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants/dot-experiments-configuration-variants.component';
import {
    ConfigurationViewModel,
    DotExperimentsConfigurationStore
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotExperimentsExperimentSummaryComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import {
    DotExperimentsConfigurationStoreMock,
    getExperimentMock
} from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessagePipe } from '@tests/dot-message-mock.pipe';

import { DotExperimentsConfigurationComponent } from './dot-experiments-configuration.component';

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

const EXPERIMENT_MOCK = getExperimentMock(0);

const defaultVmMock: ConfigurationViewModel = {
    experiment: EXPERIMENT_MOCK,
    stepStatusSidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false,
        experimentStep: null
    },
    isLoading: false,
    isExperimentADraft: false,
    disabledStartExperiment: false,
    showExperimentSummary: false,
    isSaving: false,
    experimentStatus: null
};

describe('DotExperimentsConfigurationComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationComponent>;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [
            ButtonModule,
            DotExperimentsUiHeaderComponent,
            DotExperimentsConfigurationGoalsComponent,
            DotExperimentsConfigurationTargetingComponent,
            DotExperimentsConfigurationVariantsComponent,
            DotExperimentsConfigurationTrafficComponent,
            DotExperimentsConfigurationSchedulingComponent,
            DotExperimentsConfigurationSkeletonComponent,
            DotExperimentsExperimentSummaryComponent,
            DotMessagePipe,
            TagModule,
            CardModule
        ],
        component: DotExperimentsConfigurationComponent,

        componentProviders: [
            mockProvider(DotExperimentsConfigurationStore, DotExperimentsConfigurationStoreMock)
        ],
        providers: [
            ConfirmationService,
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
            mockProvider(DotHttpErrorManagerService),
            mockProvider(Title)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.returnValue(of(EXPERIMENT_MOCK));
    });

    it('should show the skeleton component when is loading', () => {
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationSkeletonComponent)).toExist();
    });

    it('should load all the components', () => {
        spectator.component.vm$ = of({ ...defaultVmMock });
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsExperimentSummaryComponent)).not.toExist();
        expect(spectator.query(DotExperimentsConfigurationVariantsComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationGoalsComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationTargetingComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationTrafficComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationSchedulingComponent)).toExist();
    });

    it('should show Start Experiment button if isExperimentADraft true', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            isExperimentADraft: true
        });
        spectator.detectChanges();
        expect(spectator.query(byTestId('start-experiment-button'))).toExist();
    });

    it("shouldn't show Start Experiment button if isExperimentADraft false", () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            isExperimentADraft: false
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('start-experiment-button'))).not.toExist();
    });

    it('should show Start Experiment button disabled if disabledStartExperiment true', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            isExperimentADraft: true,
            disabledStartExperiment: true
        });
        spectator.detectChanges();

        const startButton = spectator.query(
            byTestId('start-experiment-button')
        ) as HTMLButtonElement;

        expect(startButton.disabled).toBeTrue();
    });

    it('should show Experiment Summary component if showExperimentSummary is true', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            showExperimentSummary: true
        });
        spectator.detectChanges();
        expect(spectator.query(DotExperimentsExperimentSummaryComponent)).toExist();
    });

    it("shouldn't show Experiment Summary component if showExperimentSummary false", () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            showExperimentSummary: false
        });
        spectator.detectChanges();
        expect(spectator.query(DotExperimentsExperimentSummaryComponent)).not.toExist();
    });
});

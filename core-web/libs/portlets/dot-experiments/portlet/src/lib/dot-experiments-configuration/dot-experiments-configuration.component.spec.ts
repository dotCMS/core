import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmPopup } from 'primeng/confirmpopup';

import { DotMessageService, DotSessionStorageService } from '@dotcms/data-access';
import { ComponentStatus, DotExperimentStatusList, PROP_NOT_FOUND } from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsInlineEditTextComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-inline-edit-text/dot-experiments-inline-edit-text.component';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationGoalsComponent } from './components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationSchedulingComponent } from './components/dot-experiments-configuration-scheduling/dot-experiments-configuration-scheduling.component';
import { DotExperimentsConfigurationSkeletonComponent } from './components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsConfigurationTrafficComponent } from './components/dot-experiments-configuration-traffic/dot-experiments-configuration-traffic.component';
import { DotExperimentsConfigurationVariantsComponent } from './components/dot-experiments-configuration-variants/dot-experiments-configuration-variants.component';
import { DotExperimentsConfigurationComponent } from './dot-experiments-configuration.component';
import {
    ConfigurationViewModel,
    DotExperimentsConfigurationStore
} from './store/dot-experiments-configuration-store';

import { DotExperimentsExperimentSummaryComponent } from '../shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

const EXPERIMENT_MOCK = getExperimentMock(0);

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: EXPERIMENT_MOCK.id,
            pageId: '222'
        },
        data: {
            config: {
                EXPERIMENTS_MIN_DURATION: '5',
                EXPERIMENTS_MAX_DURATION: PROP_NOT_FOUND
            }
        }
    }
};

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'Scheduling'
});

const defaultVmMock: ConfigurationViewModel = {
    experiment: EXPERIMENT_MOCK,
    stepStatusSidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false,
        experimentStep: null
    },
    isLoading: false,
    isExperimentADraft: false,
    runExperimentBtnLabel: '',
    disabledStartExperiment: false,
    showExperimentSummary: false,
    isSaving: false,
    experimentStatus: null,
    isDescriptionSaving: false
};

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: `p-confirmPopup`,
    template: `ConfirmPopupMockComponent`,
    standalone: true
})
export class ConfirmPopupMockComponent {}

describe('DotExperimentsConfigurationComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationComponent>;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let dotExperimentsConfigurationStore: SpyObject<DotExperimentsConfigurationStore>;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurationComponent,
        componentProviders: [DotExperimentsConfigurationStore],
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
            mockProvider(MessageService),
            mockProvider(Router),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(Title),
            mockProvider(DotSessionStorageService),
            DotMessagePipe
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        dotExperimentsService = spectator.inject(DotExperimentsService);

        dotExperimentsConfigurationStore = spectator.inject(DotExperimentsConfigurationStore, true);

        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());
    });

    it('should show the skeleton component when is loading', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            isLoading: true
        });
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationSkeletonComponent)).toExist();
    });

    it('should load all the components', () => {
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsExperimentSummaryComponent)).not.toExist();
        expect(spectator.query(DotExperimentsConfigurationVariantsComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationGoalsComponent)).toExist();
        // Wait until is implemented.
        // expect(spectator.query(DotExperimentsConfigurationTargetingComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationTrafficComponent)).toExist();
        expect(spectator.query(DotExperimentsConfigurationSchedulingComponent)).toExist();
        expect(spectator.query(DotExperimentsInlineEditTextComponent)).toExist();
    });

    it('should show Start Experiment button if isExperimentADraft true', () => {
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

    it('should show Stop Experiment button if experiment status is running and call stopExperiment after confirmation', () => {
        jest.spyOn(dotExperimentsConfigurationStore, 'stopExperiment');
        dotExperimentsService.stop.mockReturnValue(of());

        spectator.component.vm$ = of({
            ...defaultVmMock,
            experimentStatus: DotExperimentStatusList.RUNNING
        });
        spectator.detectChanges();

        spectator.click(byTestId('stop-experiment-button'));
        spectator.query(ConfirmPopup).accept();

        expect(dotExperimentsConfigurationStore.stopExperiment).toHaveBeenCalledWith(
            EXPERIMENT_MOCK
        );
    });

    it('should show hide stop Experiment button if experiment status is different than running', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            experimentStatus: DotExperimentStatusList.DRAFT
        });
        spectator.detectChanges();
        expect(spectator.query(byTestId('stop-experiment-button'))).not.toExist();
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

        expect(startButton.disabled).toEqual(true);
    });
});

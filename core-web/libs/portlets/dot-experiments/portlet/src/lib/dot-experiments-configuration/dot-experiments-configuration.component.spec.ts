import { createFakeEvent } from '@ngneat/spectator';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmPopup } from 'primeng/confirmpopup';
import { Menu } from 'primeng/menu';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { ComponentStatus, PROP_NOT_FOUND } from '@dotcms/dotcms-models';
import { DotAddToBundleComponent } from '@dotcms/ui';
import {
    DotMessagePipe,
    getExperimentMock,
    PARENT_RESOLVERS_ACTIVE_ROUTE_DATA
} from '@dotcms/utils-testing';

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

import { DotExperimentsExperimentSummaryComponent } from '../dot-experiments-reports/components/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotExperimentsInlineEditTextComponent } from '../shared/ui/dot-experiments-inline-edit-text/dot-experiments-inline-edit-text.component';

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
    },
    parent: { ...PARENT_RESOLVERS_ACTIVE_ROUTE_DATA }
};

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
    experimentStatus: null,
    isDescriptionSaving: false,
    menuItems: null,
    addToBundleContentId: null,
    disabledTooltipLabel: null
};

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: `p-confirmPopup`,
    template: `
        ConfirmPopupMockComponent
    `
})
export class ConfirmPopupMockComponent {}

describe('DotExperimentsConfigurationComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationComponent>;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let dotExperimentsConfigurationStore: SpyObject<DotExperimentsConfigurationStore>;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurationComponent,
        componentProviders: [DotExperimentsConfigurationStore],
        imports: [
            MockComponent(DotAddToBundleComponent),
            MockComponent(DotExperimentsConfigurationSchedulingComponent),
            DotMessagePipe
        ],
        providers: [
            ConfirmationService,
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            mockProvider(DotMessageService),
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(Router),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(Title),
            mockProvider(DotSessionStorageService)
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

    afterEach(() => {
        jest.resetAllMocks();
        jest.restoreAllMocks();
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

    it("shouldn't show Start Experiment button if isExperimentADraft false", () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            isExperimentADraft: false
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('start-experiment-button'))).not.toExist();
    });

    it('shouldn disable edit the name if there is an error label', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            isExperimentADraft: true,
            disabledTooltipLabel: 'error'
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('start-experiment-button'))).not.toExist();
        expect(spectator.query(DotExperimentsInlineEditTextComponent).disabled).toEqual(true);
    });

    it('should show End Experiment after confirmation', () => {
        jest.spyOn(dotExperimentsConfigurationStore, 'stopExperiment');
        dotExperimentsService.stop.mockReturnValue(of());

        spectator.detectChanges();

        expect(spectator.query(byTestId('experiment-button-menu'))).toExist();

        spectator.dispatchMouseEvent(spectator.query(byTestId('experiment-button-menu')), 'click');
        spectator.detectComponentChanges();

        expect(spectator.query(Menu)).toExist();
        spectator.query(Menu).model[1].command({ originalEvent: createFakeEvent('click') });

        spectator.query(ConfirmDialog).accept();

        expect(dotExperimentsConfigurationStore.stopExperiment).toHaveBeenCalledWith(
            EXPERIMENT_MOCK
        );
    });

    it('should show and remove add to bundle dialog', () => {
        spectator.detectChanges();

        spectator.dispatchMouseEvent(spectator.query(byTestId('experiment-button-menu')), 'click');
        spectator.detectComponentChanges();

        //Add to bundle
        spectator.query(Menu).model[5].command({ originalEvent: createFakeEvent('click') });

        spectator.detectComponentChanges();

        const addToBundle = spectator.query(DotAddToBundleComponent);

        expect(addToBundle.assetIdentifier).toEqual(EXPERIMENT_MOCK.id);

        addToBundle.cancel.emit(true);

        spectator.detectComponentChanges();

        expect(spectator.query(DotAddToBundleComponent)).not.toExist();
    });

    it('should un schedule the experiment after confirmation', () => {
        jest.spyOn(dotExperimentsConfigurationStore, 'cancelSchedule');

        spectator.detectChanges();

        expect(spectator.query(byTestId('experiment-button-menu'))).toExist();

        spectator.dispatchMouseEvent(spectator.query(byTestId('experiment-button-menu')), 'click');
        spectator.detectComponentChanges();

        expect(spectator.query(Menu)).toExist();
        spectator.query(Menu).model[3].command({ originalEvent: createFakeEvent('click') });

        spectator.query(ConfirmDialog).accept();

        expect(dotExperimentsConfigurationStore.cancelSchedule).toHaveBeenCalledWith(
            EXPERIMENT_MOCK
        );
    });
});

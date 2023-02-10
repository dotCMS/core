import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Calendar } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { Sidebar } from 'primeng/sidebar';

import { DotMessageService } from '@dotcms/data-access';
import { ExperimentSteps } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ExperimentMocks } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationSchedulingAddComponent } from './dot-experiments-configuration-scheduling-add.component';

const messageServiceMock = new MockDotMessageService({
    Done: 'Done',
    'experiments.configure.scheduling.start.time': 'Start Time',
    'experiments.configure.scheduling.end.time': 'End Time',
    'experiments.configure.scheduling.name': 'Scheduling'
});

const EXPERIMENT_ID = ExperimentMocks[0].id;

describe('DotExperimentsConfigurationSchedulingAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationSchedulingAddComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Sidebar;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationSchedulingAddComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.returnValue(of(ExperimentMocks[0]));

        store.loadExperiment(EXPERIMENT_ID);
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.SCHEDULING,
            isOpen: true
        });
        spectator.detectChanges();
    });

    it('should load scheduling current values', () => {
        const startDateCalendar: Calendar = spectator.query(Calendar);
        const endDateCalendar: Calendar = spectator.queryLast(Calendar);

        expect(startDateCalendar.value.getTime()).toEqual(ExperimentMocks[0].scheduling.startDate);
        expect(endDateCalendar.value.getTime()).toEqual(ExperimentMocks[0].scheduling.endDate);
    });

    it('should save form when is valid', () => {
        spyOn(store, 'setSelectedScheduling');
        const submitButton = spectator.query(
            byTestId('add-scheduling-button')
        ) as HTMLButtonElement;

        expect(submitButton.disabled).toBeFalse();
        expect(submitButton).toContainText('Done');
        expect(spectator.component.form.valid).toBeTrue();

        spectator.click(submitButton);
        expect(store.setSelectedScheduling).toHaveBeenCalledWith({
            scheduling: ExperimentMocks[0].scheduling,
            experimentId: EXPERIMENT_ID
        });
    });

    it('should close sidebar', () => {
        spyOn(store, 'closeSidebar');
        sidebar = spectator.query(Sidebar);
        sidebar.hide();

        expect(store.closeSidebar).toHaveBeenCalledTimes(1);
    });

    //TODO: Will be tested once defined the rules of edit.
    // eslint-disable-next-line
    xit('should not submit the form when invalid', () => {});
});

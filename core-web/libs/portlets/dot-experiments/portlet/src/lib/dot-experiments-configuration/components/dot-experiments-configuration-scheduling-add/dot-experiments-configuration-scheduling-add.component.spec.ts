import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePicker } from 'primeng/datepicker';
import { Drawer } from 'primeng/drawer';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ExperimentSteps, TIME_90_DAYS } from '@dotcms/dotcms-models';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationSchedulingAddComponent } from './dot-experiments-configuration-scheduling-add.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

const messageServiceMock = new MockDotMessageService({
    Done: 'Done',
    'experiments.configure.scheduling.start.time': 'Start Time',
    'experiments.configure.scheduling.end.time': 'End Time',
    'experiments.configure.scheduling.name': 'Scheduling'
});

const EXPERIMENT_MOCK = {
    ...getExperimentMock(0),
    scheduling: { startDate: 1, endDate: 12196e5 }
};
const MOCK_DATA_MILLISECONDS = 16820996334200;
const MOCK_DATE = new Date(MOCK_DATA_MILLISECONDS);
describe('DotExperimentsConfigurationSchedulingAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationSchedulingAddComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Drawer;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationSchedulingAddComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(ConfirmationService)
        ]
    });

    beforeEach(async () => {
        jest.useFakeTimers();
        jest.setSystemTime(MOCK_DATE);
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.SCHEDULING,
            isOpen: true
        });
        spectator.detectChanges();
    });

    it('should load scheduling current values', () => {
        const startDateCalendar: DatePicker = spectator.query(DatePicker);
        const endDateCalendar: DatePicker = spectator.queryLast(DatePicker);

        expect(startDateCalendar.value.getTime()).toEqual(EXPERIMENT_MOCK.scheduling.startDate);
        expect(endDateCalendar.value.getTime()).toEqual(EXPERIMENT_MOCK.scheduling.endDate);
    });

    it('should have set the props correctly', () => {
        const startDateCalendar: DatePicker = spectator.query(DatePicker);
        const endDateCalendar: DatePicker = spectator.queryLast(DatePicker);

        expect(startDateCalendar.stepMinute).toEqual(30);
        expect(startDateCalendar.readonlyInput).toEqual(true);
        expect(startDateCalendar.showIcon).toEqual(true);
        expect(startDateCalendar.showClear).toEqual(true);

        expect(endDateCalendar.stepMinute).toEqual(30);
        expect(endDateCalendar.readonlyInput).toEqual(true);
        expect(endDateCalendar.showIcon).toEqual(true);
        expect(endDateCalendar.showClear).toEqual(true);
    });

    it('should save form when is valid', () => {
        jest.spyOn(store, 'setSelectedScheduling');
        const submitButtonWrapper = spectator.query(byTestId('add-scheduling-button'));
        const submitButton = submitButtonWrapper.querySelector('button') || submitButtonWrapper;

        expect(submitButton.hasAttribute('disabled')).toBe(false);
        expect(submitButtonWrapper).toContainText('Done');
        expect(spectator.component.form.valid).toEqual(true);

        spectator.click(submitButton);
        expect(store.setSelectedScheduling).toHaveBeenCalledWith({
            scheduling: EXPERIMENT_MOCK.scheduling,
            experimentId: EXPERIMENT_MOCK.id
        });
    });

    it('should set min dates correctly', () => {
        const startDateCalendar: DatePicker = spectator.query(DatePicker);
        const endDateCalendar: DatePicker = spectator.queryLast(DatePicker);
        const component = spectator.component;
        const time5days = 432e6; // value set in the ActiveRouteMock
        const mockMinEndDate = MOCK_DATA_MILLISECONDS + time5days;

        component.form.get('startDate').setValue(new Date());
        startDateCalendar.onSelect.emit();

        spectator.detectChanges();

        expect(endDateCalendar.minDate.getTime()).toEqual(mockMinEndDate);
        expect(endDateCalendar.defaultDate.getTime()).toEqual(mockMinEndDate);
    });

    it('should clear end date if start date is equal or more', () => {
        const startDateCalendar: DatePicker = spectator.query(DatePicker);
        const component = spectator.component;

        component.form.get('startDate').setValue(new Date());
        component.form.get('endDate').setValue(new Date());
        startDateCalendar.onSelect.emit();

        spectator.detectChanges();

        expect(component.form.get('endDate').value).toEqual(null);
    });

    it('max end date date should be 90 days', () => {
        const startDateCalendar: DatePicker = spectator.query(DatePicker);
        const component = spectator.component;
        // Default vale of 90 because max end date is not defined in the Active Route
        const expectedEndDate = new Date(MOCK_DATA_MILLISECONDS + TIME_90_DAYS);

        component.form.get('startDate').setValue(new Date());
        startDateCalendar.onSelect.emit();

        spectator.detectChanges();

        expect(component.maxEndDate).toEqual(expectedEndDate);
    });

    it('should close sidebar', () => {
        jest.spyOn(store, 'closeSidebar');
        sidebar = spectator.query(Drawer);
        sidebar.hide();

        expect(store.closeSidebar).toHaveBeenCalledTimes(1);
    });

    afterEach(() => {
        jest.useRealTimers();
    });
});

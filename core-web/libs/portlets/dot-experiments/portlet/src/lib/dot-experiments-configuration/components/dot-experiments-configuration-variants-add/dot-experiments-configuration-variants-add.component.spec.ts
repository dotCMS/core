import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus, ExperimentSteps } from '@dotcms/dotcms-models';
import { ACTIVE_ROUTE_MOCK_CONFIG } from '@dotcms/utils-testing';

import { DotExperimentsConfigurationVariantsAddComponent } from './dot-experiments-configuration-variants-add.component';

import { DotExperimentsReportsChartComponent } from '../../../dot-experiments-reports/components/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

describe('DotExperimentsConfigurationVariantsAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsAddComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [MockComponent(DotExperimentsReportsChartComponent)],
        component: DotExperimentsConfigurationVariantsAddComponent,
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),

            mockProvider(MessageService),
            mockProvider(DotMessageService),
            mockProvider(ConfirmationService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.addVariant.mockReturnValue(of());

        spectator.component.vm$ = of({
            experimentId: '1',
            trafficProportion: null,
            status: {
                status: ComponentStatus.IDLE,
                isOpen: true,
                experimentStep: ExperimentSteps.VARIANTS
            },
            isExperimentADraft: true,
            canLockPage: true,
            pageSate: null,
            disabledTooltipLabel: null
        });

        spectator.detectChanges();
    });

    it('should have a form', () => {
        expect(spectator.query(byTestId('new-variant-form'))).toExist();
    });

    it('should saveForm when form is valid', async () => {
        jest.spyOn(store, 'addVariant');

        const formValues = {
            name: 'name'
        };

        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();
        spectator.detectComponentChanges();

        await spectator.fixture.whenStable();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;

        expect(submitButton.disabled).toEqual(false);
        expect(spectator.component.form.valid).toEqual(true);

        spectator.click(submitButton);

        expect(store.addVariant).toHaveBeenCalledWith({
            name: 'name',
            experimentId: '1'
        });
    });

    it('should disable submit button if the form is invalid', async () => {
        const invalidFormValues = {
            name: 'this is more than 50 characters test - this is more than 50 characters test'
        };

        spectator.component.form.setValue(invalidFormValues);
        spectator.component.form.updateValueAndValidity();
        spectator.detectComponentChanges();

        await spectator.fixture.whenStable();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;
        expect(submitButton.disabled).toEqual(true);
    });
});

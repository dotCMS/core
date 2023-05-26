import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SidebarModule } from 'primeng/sidebar';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, ExperimentSteps } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ACTIVE_ROUTE_MOCK_CONFIG } from '@portlets/dot-experiments/test/mocks';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

import { DotExperimentsConfigurationVariantsAddComponent } from './dot-experiments-configuration-variants-add.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.create.form.sidebar.header': 'Add a new experiment',
    'experiments.action.add': 'Add'
});

describe('DotExperimentsConfigurationVariantsAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsAddComponent>;
    let store: DotExperimentsConfigurationStore;

    const createComponent = createComponentFactory({
        imports: [
            CommonModule,
            ReactiveFormsModule,

            SidebarModule,
            DotSidebarDirective,
            DotSidebarHeaderComponent,
            ButtonModule,
            InputTextModule,
            InputTextareaModule,
            DotFieldValidationMessageModule
        ],
        component: DotExperimentsConfigurationVariantsAddComponent,
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(MessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);

        spectator.component.vm$ = of({
            experimentId: '1',
            trafficProportion: null,
            status: {
                status: ComponentStatus.IDLE,
                isOpen: true,
                experimentStep: ExperimentSteps.VARIANTS
            },
            isExperimentADraft: true
        });

        spectator.detectChanges();
    });

    it('should have a form', () => {
        expect(spectator.query(byTestId('new-variant-form'))).toExist();
    });

    it('should saveForm when form is valid', () => {
        spyOn(store, 'addVariant');

        const formValues = {
            name: 'name'
        };

        spectator.component.form.setValue(formValues);
        spectator.detectComponentChanges();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;
        expect(submitButton.disabled).toBeFalse();

        expect(submitButton).toContainText('Add');
        expect(spectator.component.form.valid).toBeTrue();

        spectator.click(submitButton);

        expect(store.addVariant).toHaveBeenCalledWith({ name: 'name', experimentId: '1' });
    });

    it('should disable submit button if the form is invalid', () => {
        const invalidFormValues = {
            name: 'this is more than 50 characters test - this is more than 50 characters test'
        };

        spectator.component.form.setValue(invalidFormValues);
        spectator.component.form.updateValueAndValidity();
        spectator.detectComponentChanges();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;

        expect(submitButton.disabled).toBeTrue();
        expect(submitButton).toContainText('Add');
    });
});

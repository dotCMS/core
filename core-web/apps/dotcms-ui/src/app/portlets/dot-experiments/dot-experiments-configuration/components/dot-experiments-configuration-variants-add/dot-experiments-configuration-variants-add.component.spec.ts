import { DotExperimentsConfigurationVariantsAddComponent } from './dot-experiments-configuration-variants-add.component';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessageService } from '@dotcms/data-access';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

const messageServiceMock = new MockDotMessageService({
    'experiments.create.form.sidebar.header': 'Add a new experiment',
    'experiments.action.add': 'Add'
});

describe('DotExperimentsConfigurationVariantsAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsAddComponent>;

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
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(MessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: { isSidebarOpen: true }
        });
    });

    it('should have a form', () => {
        expect(spectator.query(byTestId('new-variant-form'))).toExist();
    });

    it('should call saveForm() on add button click and form valid', () => {
        let formValuesOutput;

        const formValues = {
            name: 'name'
        };

        spectator.component.form.setValue(formValues);
        spectator.detectComponentChanges();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;
        expect(submitButton.disabled).toBeFalse();

        expect(submitButton).toContainText('Add');
        expect(spectator.component.form.valid).toBeTrue();

        spectator.output('formValues').subscribe((result) => (formValuesOutput = result));

        spectator.click(submitButton);

        expect(formValuesOutput).toEqual(formValues);
    });

    it('should disable submit button if the form is invalid', () => {
        const invalidFormValues = {
            name: ''
        };

        spectator.component.form.setValue(invalidFormValues);
        spectator.component.form.updateValueAndValidity();
        spectator.detectComponentChanges();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;

        expect(submitButton.disabled).toBeTrue();
        expect(submitButton).toContainText('Add');
    });
});

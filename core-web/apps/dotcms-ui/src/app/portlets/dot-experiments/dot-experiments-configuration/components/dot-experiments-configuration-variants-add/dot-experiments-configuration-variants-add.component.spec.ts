import { DotExperimentsConfigurationVariantsAddComponent } from './dot-experiments-configuration-variants-add.component';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MessageService } from 'primeng/api';
import {
    DotExperimentsConfigurationStoreMock,
    ExperimentMocks
} from '@portlets/dot-experiments/test/mocks';
import { ButtonModule } from 'primeng/button';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { of } from 'rxjs';

const messageServiceMock = new MockDotMessageService({
    'experiments.create.form.sidebar.header': 'Add a new experiment',
    'experiments.action.add': 'Add'
});

describe('DotExperimentsConfigurationVariantsAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsAddComponent>;
    let dotExperimentsConfigurationStore: SpyObject<DotExperimentsConfigurationStore>;

    const createComponent = createComponentFactory({
        imports: [
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
            mockProvider(DotExperimentsConfigurationStore, DotExperimentsConfigurationStoreMock),
            mockProvider(MessageService),
            mockProvider(DotExperimentsService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        dotExperimentsConfigurationStore = spectator.inject(DotExperimentsConfigurationStore);

        spectator.component.vm$ = of({
            isSidebarOpen: true,
            isSaving: false,
            trafficProportion: ExperimentMocks[0].trafficProportion,
            isVariantStepDone: false
        });
        spectator.detectChanges();
    });

    it('should have a form', () => {
        expect(spectator.query(byTestId('new-variant-form'))).toExist();
    });
    it('should call handleSubmit() on add button click and form valid', () => {
        spyOn(dotExperimentsConfigurationStore, 'openSidebar');

        spectator.component.ngOnInit();
        expect(dotExperimentsConfigurationStore.openSidebar).toHaveBeenCalled();

        const formValues = {
            name: 'name'
        };

        spectator.component.form.setValue(formValues);
        spectator.detectComponentChanges();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;

        expect(submitButton.disabled).toBeFalse();
        expect(submitButton).toContainText('Add');
        expect(spectator.component.form.valid).toBeTrue();

        const handleSubmitSpy = spyOn(spectator.component, 'handleSubmit');
        spectator.click(submitButton);
        spectator.detectComponentChanges();

        expect(handleSubmitSpy).toHaveBeenCalled();
    });
    it('should disable submit button if the form is invalid', () => {
        spectator.component.ngOnInit();
        const invalidFormValues = {
            name: ''
        };

        spectator.component.form.setValue(invalidFormValues);
        spectator.component.form.updateValueAndValidity();
        spectator.detectComponentChanges();

        const submitButton = spectator.query(byTestId('add-variant-button')) as HTMLButtonElement;

        expect(submitButton.disabled).toBeTrue();
        expect(submitButton).toContainText('Add');
        expect(spectator.component.form.valid).toBeFalse();

        expect(spectator.component.form.controls.name.valid).toBeFalse();
    });
});

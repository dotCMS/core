import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { Sidebar, SidebarModule } from 'primeng/sidebar';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsCreateComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-create/dot-experiments-create.component';
import { DotExperimentsListStore } from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotExperimentsListStoreMock } from '@portlets/dot-experiments/test/mocks';
import {
    DotSidebarDirective,
    SIDEBAR_PLACEMENT,
    SIDEBAR_SIZES
} from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.create.form.sidebar.header': 'Add a new experiment',
    'experiments.create.form.name.label': 'Name Label',
    'experiments.create.form.name.placeholder': 'Name placeholder',
    'experiments.create.form.description.label': 'Description Label',
    'experiments.create.form.description.placeholder': 'Description Placeholder',
    'experiments.action.add': 'Add',
    'error.form.validator.maxlength': 'maxlength error',
    'error.form.validator.required': 'required error'
});

const dotExperimentsServiceMock = {
    add: (experiment) => of({ entity: experiment })
};

describe('DotExperimentsCreateComponent', () => {
    let spectator: Spectator<DotExperimentsCreateComponent>;

    let primeNgSidebar: Sidebar;
    let dotSidebarHeaderComponent: DotSidebarHeaderComponent;
    let dotSidebarDirective: DotSidebarDirective;

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
        component: DotExperimentsCreateComponent,
        providers: [
            MessageService,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },

            {
                provide: DotExperimentsListStore,
                useValue: DotExperimentsListStoreMock
            },

            mockProvider(DotExperimentsService, dotExperimentsServiceMock)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should has Sidebar Component (PrimeNg) and DotSidebarDirective', () => {
        const SIDEBAR_CONFIG_BY_DOTSIDEBAR_DIRECTIVE = {
            position: SIDEBAR_PLACEMENT.RIGHT,
            styleClass: SIDEBAR_SIZES.MD,
            showCloseIcon: false
        };

        spectator.detectChanges();

        primeNgSidebar = spectator.query(Sidebar);
        dotSidebarDirective = spectator.query(DotSidebarDirective);

        expect(primeNgSidebar).toExist();
        expect(dotSidebarDirective).toExist();

        expect(primeNgSidebar.position).toBe(SIDEBAR_CONFIG_BY_DOTSIDEBAR_DIRECTIVE.position);
        expect(primeNgSidebar.styleClass).toBe(SIDEBAR_CONFIG_BY_DOTSIDEBAR_DIRECTIVE.styleClass);
        expect(primeNgSidebar.showCloseIcon).toBe(
            SIDEBAR_CONFIG_BY_DOTSIDEBAR_DIRECTIVE.showCloseIcon
        );
    });
    it('should has DotSidebarHeaderComponent', () => {
        dotSidebarHeaderComponent = spectator.query(DotSidebarHeaderComponent);
        expect(dotSidebarHeaderComponent).toExist();
    });

    it('should open the sidebar', () => {
        primeNgSidebar = spectator.query(Sidebar);
        expect(primeNgSidebar.visible).toBe(true);
    });

    it('submit should call handleSubmit()', () => {
        const submitButton = spectator.query<HTMLButtonElement>(byTestId('add-experiment-button'));
        spyOn(spectator.component, 'handleSubmit');

        spectator.component.handleSubmit('1111-1111-1111-111');

        expect(submitButton).toExist();
        expect(spectator.component.handleSubmit).toHaveBeenCalled();
    });

    describe('Form', () => {
        it('should have a form', () => {
            expect(spectator.query(byTestId('new-experiment-form'))).toExist();
        });
        it('should call handleSubmit() on Add button click and form valid', () => {
            spectator.component.ngOnInit();
            const formValues = {
                name: 'name',
                description: 'experiment description',
                pageId: '1111-1111-1111-1111'
            };

            spectator.component.form.setValue(formValues);
            spectator.detectComponentChanges();

            const submitButton = spectator.query(
                byTestId('add-experiment-button')
            ) as HTMLButtonElement;

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
                name: '',
                description: '',
                pageId: '1111-1111-1111-1111'
            };

            spectator.component.form.setValue(invalidFormValues);
            spectator.component.form.updateValueAndValidity();
            spectator.detectComponentChanges();

            const submitButton = spectator.query(
                byTestId('add-experiment-button')
            ) as HTMLButtonElement;

            expect(submitButton.disabled).toBeTrue();
            expect(submitButton).toContainText('Add');
            expect(spectator.component.form.valid).toBeFalse();

            expect(spectator.component.form.controls.name.valid).toBeFalse();
        });
    });
});

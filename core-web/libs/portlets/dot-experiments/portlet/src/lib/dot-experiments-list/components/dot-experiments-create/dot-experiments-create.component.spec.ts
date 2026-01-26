import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';

import { DotExperimentsService, DotMessageService } from '@dotcms/data-access';
import {
    DotFieldValidationMessageComponent,
    DotSidebarDirective,
    DotSidebarHeaderComponent,
    SIDEBAR_PLACEMENT
} from '@dotcms/ui';
import { DotExperimentsListStoreMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsCreateComponent } from './dot-experiments-create.component';

import { DotExperimentsListStore } from '../../store/dot-experiments-list-store';

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

    let primeNgSidebar: Drawer;
    let dotSidebarHeaderComponent: DotSidebarHeaderComponent;
    let dotSidebarDirective: DotSidebarDirective;

    const createComponent = createComponentFactory({
        imports: [
            DrawerModule,
            DotSidebarDirective,
            DotSidebarHeaderComponent,
            ButtonModule,
            InputTextModule,
            TextareaModule,
            DotFieldValidationMessageComponent
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
        spectator.detectChanges();

        primeNgSidebar = spectator.query(Drawer);
        dotSidebarDirective = spectator.query(DotSidebarDirective);

        expect(primeNgSidebar).toExist();
        expect(dotSidebarDirective).toExist();

        // Check properties set by the template
        expect(primeNgSidebar.position()).toBe(SIDEBAR_PLACEMENT.RIGHT);
        expect(primeNgSidebar.closable).toBe(false);

        // Check properties set by DotSidebarDirective
        expect(primeNgSidebar.dismissible).toBe(false);
        expect(primeNgSidebar.closeOnEscape).toBe(false);
    });
    it('should has DotSidebarHeaderComponent', () => {
        dotSidebarHeaderComponent = spectator.query(DotSidebarHeaderComponent);
        expect(dotSidebarHeaderComponent).toExist();
    });

    it('should open the sidebar', () => {
        primeNgSidebar = spectator.query(Drawer);
        expect(primeNgSidebar.visible).toBe(true);
    });

    it('submit should call handleSubmit()', () => {
        const submitButton = spectator.query<HTMLButtonElement>(byTestId('add-experiment-button'));
        jest.spyOn(spectator.component, 'handleSubmit');

        spectator.component.handleSubmit('1111-1111-1111-111');

        expect(submitButton).toExist();
        expect(spectator.component.handleSubmit).toHaveBeenCalled();
    });

    describe('Form', () => {
        it('should have a form & autofocus ', () => {
            expect(spectator.query(byTestId('new-experiment-form'))).toExist();
            expect(spectator.query(byTestId('add-experiment-name-input'))).toHaveAttribute(
                'dotAutofocus'
            );
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

            expect(submitButton.disabled).toEqual(false);
            expect(submitButton).toContainText('Add');
            expect(spectator.component.form.valid).toEqual(true);

            const handleSubmitSpy = jest.spyOn(spectator.component, 'handleSubmit');
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

            expect(submitButton.disabled).toEqual(true);
            expect(submitButton).toContainText('Add');
            expect(spectator.component.form.valid).toEqual(false);

            expect(spectator.component.form.controls.name.valid).toEqual(false);
        });
    });
});

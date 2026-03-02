import { createComponentFactory, Spectator, byTestId, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { LoginService, SiteService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import {
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockDotPersona,
    mockResponseView,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotAddPersonaDialogComponent } from './dot-add-persona-dialog.component';

const messageServiceMock = new MockDotMessageService({
    'modes.persona.add.persona': 'Add Persona',
    'dot.common.dialog.accept': 'Accept',
    'dot.common.dialog.reject': 'Cancel'
});

describe('DotAddPersonaDialogComponent', () => {
    let spectator: Spectator<DotAddPersonaDialogComponent>;

    const createComponent = createComponentFactory({
        component: DotAddPersonaDialogComponent,
        imports: [BrowserAnimationsModule],
        detectChanges: false,
        providers: [
            DotWorkflowActionsFireService,
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn().mockReturnValue(of(undefined))
            }),
            {
                provide: DotMessageDisplayService,
                useClass: DotMessageDisplayServiceMock
            },
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: SiteService, useValue: new SiteServiceMock() },
            mockProvider(GlobalStore, { currentSiteId: jest.fn().mockReturnValue('demo') })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    afterEach(() => {
        if (spectator) {
            spectator.setInput('visible', false);
            spectator.detectChanges();
        }
    });

    it('should not be visible by default', () => {
        expect(spectator.query('p-dialog')).toBeNull();
    });

    describe('when dialog is visible', () => {
        beforeEach(() => {
            spectator.setInput('visible', true);
            spectator.detectChanges();
        });

        it('should render the dialog', () => {
            expect(spectator.query(byTestId('dot-add-persona-dialog'))).toBeTruthy();
            expect(spectator.query('p-dialog')).toBeTruthy();
        });

        it('should pass personaName to the dot-create-persona-form', () => {
            spectator.setInput('personaName', 'Test');
            spectator.detectChanges();

            expect(spectator.component.personaForm?.personaName).toEqual('Test');
        });

        it('should set dialog actions with correct labels and initial state', () => {
            expect(spectator.component.dialogActions).toBeDefined();
            expect(spectator.component.dialogActions.accept.label).toEqual('Accept');
            expect(spectator.component.dialogActions.accept.disabled).toBe(true);
            expect(spectator.component.dialogActions.accept.action).toEqual(expect.any(Function));
            expect(spectator.component.dialogActions.cancel.label).toEqual('Cancel');
            expect(spectator.component.dialogActions.cancel.action).toEqual(expect.any(Function));
        });

        it('should enable accept button when form becomes valid', () => {
            spectator.triggerEventHandler('dot-create-persona-form', 'isValid', true);
            spectator.detectChanges();

            expect(spectator.component.dialogActions.accept.disabled).toBe(false);
        });

        it('should reset form, disable accept and set visible to false on closeDialog', () => {
            const formComponent = spectator.component.personaForm;
            jest.spyOn(formComponent, 'resetForm');

            spectator.component.closeDialog();

            expect(formComponent.resetForm).toHaveBeenCalled();
            expect(spectator.component.visible).toBe(false);
            expect(spectator.component.dialogActions.accept.disabled).toBe(true);
        });

        it('should call closeDialog when p-dialog visibleChange emits false', () => {
            jest.spyOn(spectator.component, 'closeDialog');

            spectator.triggerEventHandler('p-dialog', 'visibleChange', false);

            expect(spectator.component.closeDialog).toHaveBeenCalled();
        });

        describe('submit (workflow create persona)', () => {
            let dotHttpErrorManagerService: DotHttpErrorManagerService;
            let dotWorkflowActionsFireService: DotWorkflowActionsFireService;

            function submitForm(): void {
                spectator.triggerEventHandler('dot-create-persona-form', 'isValid', true);
                spectator.component.personaForm.form.setValue({
                    name: 'Freddy',
                    hostFolder: 'demo',
                    keyTag: 'freddy',
                    photo: '',
                    tags: null
                });
                // p-dialog with appendTo="body" renders footer outside fixture; query from document.body
                const acceptEl = document.body.querySelector(
                    '[data-testid="dot-add-persona-dialog-accept"]'
                );
                const button = acceptEl?.querySelector('button') as HTMLElement;
                if (button) {
                    button.click();
                }
                spectator.detectChanges();
            }

            beforeEach(() => {
                dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
                dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
                jest.spyOn(spectator.component.createdPersona, 'emit');
                Object.defineProperty(spectator.component.personaForm.form, 'valid', {
                    value: true,
                    writable: true
                });
            });

            it('should create persona, emit createdPersona, close dialog and disable accept when form is valid', () => {
                jest.spyOn(spectator.component, 'closeDialog');
                jest.spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).mockReturnValue(of(mockDotPersona));

                submitForm();

                expect(
                    dotWorkflowActionsFireService.publishContentletAndWaitForIndex
                ).toHaveBeenCalledWith('persona', {
                    hostFolder: 'demo',
                    keyTag: 'freddy',
                    name: 'Freddy',
                    photo: '',
                    tags: null
                });
                expect(spectator.component.createdPersona.emit).toHaveBeenCalledWith(
                    mockDotPersona
                );
                expect(spectator.component.createdPersona.emit).toHaveBeenCalledTimes(1);
                expect(spectator.component.closeDialog).toHaveBeenCalled();
                expect(spectator.component.dialogActions.accept.disabled).toBe(true);
            });

            it('should call dotHttpErrorManagerService when endpoint fails and re-enable accept button', () => {
                const fake500Response = mockResponseView(500);
                spectator.component.dialogActions.accept.disabled = true;
                jest.spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).mockReturnValue(throwError(fake500Response));

                submitForm();

                expect(spectator.component.createdPersona.emit).not.toHaveBeenCalled();
                expect(spectator.component.dialogActions.accept.disabled).toBe(false);
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(fake500Response);
            });
        });
    });
});

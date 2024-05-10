import { of as observableOf, throwError } from 'rxjs';

import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { FileUploadModule } from 'primeng/fileupload';

import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotCreatePersonaFormModule } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.module';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { LoginService, SiteService } from '@dotcms/dotcms-js';
import { DotDialogModule, DotMessagePipe } from '@dotcms/ui';
import {
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockDotPersona,
    mockResponseView,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotAddPersonaDialogComponent } from './dot-add-persona-dialog.component';

@Component({
    selector: 'dot-field-validation-message',
    template: ''
})
class TestFieldValidationMessageComponent {
    @Input() field: NgControl;
    @Input() message: string;
}

describe('DotAddPersonaDialogComponent', () => {
    let component: DotAddPersonaDialogComponent;
    let fixture: ComponentFixture<DotAddPersonaDialogComponent>;
    let dotDialog: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        'modes.persona.add.persona': 'Add Persona',
        'dot.common.dialog.accept': 'Accept',
        'dot.common.dialog.reject': 'Cancel'
    });

    beforeEach(() => {
        const siteServiceMock = new SiteServiceMock();

        DOTTestBed.configureTestingModule({
            declarations: [DotAddPersonaDialogComponent, TestFieldValidationMessageComponent],
            imports: [
                BrowserAnimationsModule,
                DotDialogModule,
                FileUploadModule,
                SiteSelectorFieldModule,
                DotCreatePersonaFormModule,
                DotMessagePipe
            ],
            providers: [
                DotWorkflowActionsFireService,
                DotHttpErrorManagerService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: SiteService, useValue: siteServiceMock }
            ]
        });

        fixture = TestBed.createComponent(DotAddPersonaDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        dotDialog = fixture.debugElement.query(By.css('dot-dialog'));
    });

    afterEach(() => {
        component.visible = false;
        fixture.detectChanges();
    });

    it('should not be visible by default', () => {
        expect(dotDialog).toBeNull();
    });

    describe('visible Dialog', () => {
        beforeEach(() => {
            component.visible = true;
            fixture.detectChanges();
            dotDialog = fixture.debugElement.query(By.css('dot-dialog'));
        });

        it('should pass personaName to the dot-persona-form', () => {
            component.personaName = 'Test';
            fixture.detectChanges();
            const personaForm = fixture.debugElement.query(By.css('dot-create-persona-form'));
            expect(personaForm.componentInstance.personaName).toEqual('Test');
        });

        it('should set dialog attributes correctly', () => {
            expect(dotDialog.componentInstance.header).toEqual('Add Persona');
            expect(dotDialog.componentInstance.appendToBody).toBe(true);
            expect(dotDialog.componentInstance.actions).toEqual({
                accept: {
                    label: 'Accept',
                    disabled: true,
                    action: jasmine.any(Function)
                },
                cancel: {
                    label: 'Cancel',
                    action: jasmine.any(Function)
                }
            });
        });

        it('should handle disable state of the accept button when form value change', () => {
            component.personaForm.isValid.emit(true);
            expect(component.dialogActions.accept.disabled).toBe(false);
        });

        it('should reset persona form, disable accept button and set visible to false on closeDialog', () => {
            spyOn(component.personaForm, 'resetForm');
            component.closeDialog();

            expect(component.personaForm.resetForm).toHaveBeenCalled();
            expect(component.visible).toBe(false);
            expect(component.dialogActions.accept.disabled).toBe(true);
        });

        it('should call closeDialog on dotDialog hide', () => {
            spyOn(component, 'closeDialog');
            dotDialog.componentInstance.hide.emit();
            expect(component.closeDialog).toHaveBeenCalled();
        });

        describe('call to dotWorkflowActionsFireService endpoint', () => {
            const submitForm = () => {
                const form = de.query(By.css('dot-create-persona-form'));
                form.triggerEventHandler('isValid', true);
                form.componentInstance.form.setValue({
                    name: 'Freddy',
                    hostFolder: 'demo',
                    keyTag: 'freddy',
                    photo: '',
                    tags: null
                });
                const accept = dialog.query(By.css('.dialog__button-accept'));
                accept.triggerEventHandler('click', {});
            };

            let dotHttpErrorManagerService: DotHttpErrorManagerService;
            let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
            let de: DebugElement;
            let dialog;

            beforeEach(() => {
                de = fixture.debugElement;
                dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);
                dotWorkflowActionsFireService = de.injector.get(DotWorkflowActionsFireService);
                spyOn(component.createdPersona, 'emit');
                spyOnProperty(component.personaForm.form, 'valid').and.returnValue(true);
                dialog = de.query(By.css('dot-dialog'));
            });

            it('should create and emit the new persona, disable accept button and close dialog if form is valid', () => {
                spyOn(component, 'closeDialog');
                spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).and.returnValue(observableOf(mockDotPersona));

                submitForm();

                fixture.detectChanges();
                expect(
                    dotWorkflowActionsFireService.publishContentletAndWaitForIndex
                ).toHaveBeenCalledWith('persona', {
                    hostFolder: 'demo',
                    keyTag: 'freddy',
                    name: 'Freddy',
                    photo: '',
                    tags: null
                });
                expect(component.createdPersona.emit).toHaveBeenCalledWith(mockDotPersona);
                expect(component.closeDialog).toHaveBeenCalled();
                expect(component.dialogActions.accept.disabled).toEqual(true);
            });

            it('should call dotHttpErrorManagerService if endpoint fails, since form is valid, accept button should not be enable', () => {
                const fake500Response = mockResponseView(500);
                spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
                component.dialogActions.accept.disabled = true;
                spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).and.returnValue(throwError(fake500Response));

                submitForm();

                expect(component.createdPersona.emit).not.toHaveBeenCalled();
                expect(component.dialogActions.accept.disabled).toEqual(false);
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(fake500Response);
            });
        });
    });
});

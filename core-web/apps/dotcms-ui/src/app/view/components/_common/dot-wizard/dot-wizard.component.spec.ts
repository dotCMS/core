/* eslint-disable @typescript-eslint/no-explicit-any */

import { CommonModule } from '@angular/common';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';

import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotContainerReferenceModule } from '@directives/dot-container-reference/dot-container-reference.module';
import { DotWizardService } from '@dotcms/app/api/services/dot-wizard/dot-wizard.service';
import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogData } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotWizardInput } from '@models/dot-wizard-input/dot-wizard-input.model';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';

import { DotWizardComponent } from './dot-wizard.component';

const messageServiceMock = new MockDotMessageService({
    send: 'Send',
    next: 'Next',
    previous: 'Previous',
    cancel: 'cancel'
});

@Component({
    selector: 'dot-form-one',
    template:
        '<form><span>name: </span><input class="formOneFirst" /><br><span>last Name:</span><input/></form>'
})
class FormOneComponent {
    @Input() data: DotPushPublishDialogData;
    @Output() value = new EventEmitter<any>();
    @Output() valid = new EventEmitter<boolean>();
}

@Component({
    selector: 'dot-form-two',
    template: '<form><input class="formTwoFirst"/></form>'
})
class FormTwoComponent {
    @Input() data: DotPushPublishDialogData;
    @Output() value = new EventEmitter<any>();
    @Output() valid = new EventEmitter<boolean>();
}

const mockSteps: DotWizardStep<any>[] = [
    { component: FormOneComponent, data: { id: 'numberOne' } },
    { component: FormTwoComponent, data: { id: 'numberTwo' } }
];

const wizardInput: DotWizardInput = {
    title: 'Test Title',
    steps: mockSteps
};

const stopImmediatePropagation = jasmine.createSpy('');

const enterEvent = {
    stopImmediatePropagation: stopImmediatePropagation
};

describe('DotWizardComponent', () => {
    let component: DotWizardComponent;
    let fixture: ComponentFixture<DotWizardComponent>;
    let dotWizardService: DotWizardService;
    let stepContainers: DebugElement[];

    let acceptButton: DebugElement;
    let closeButton: DebugElement;

    let form1: FormOneComponent;
    let form2: FormTwoComponent;
    let formsContainer: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotWizardComponent, FormOneComponent, FormTwoComponent],
            imports: [DotDialogModule, CommonModule, DotContainerReferenceModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotWizardService
            ]
        }).compileComponents();

        TestBed.overrideModule(BrowserDynamicTestingModule, {
            set: {
                entryComponents: [FormOneComponent, FormTwoComponent]
            }
        });
        TestBed.compileComponents();
    }));

    describe('multiple steps', () => {
        beforeEach(fakeAsync(() => {
            fixture = TestBed.createComponent(DotWizardComponent);
            component = fixture.componentInstance;
            fixture.detectChanges();
            dotWizardService = fixture.debugElement.injector.get(DotWizardService);
            dotWizardService.open(wizardInput);
            fixture.detectChanges();
            stepContainers = fixture.debugElement.queryAll(By.css('.dot-wizard__step'));
            tick(201); // interval time to focus first element.
            fixture.detectChanges();
            acceptButton = fixture.debugElement.query(By.css('.dialog__button-accept'));
            closeButton = fixture.debugElement.query(By.css('.dialog__button-cancel'));
            form1 = fixture.debugElement.query(By.css('dot-form-one')).componentInstance;
            form2 = fixture.debugElement.query(By.css('dot-form-two')).componentInstance;
            formsContainer = fixture.debugElement.query(By.css('.dot-wizard__container'));
        }));

        it('should set dialog params', () => {
            const dotDialog: DotDialogComponent = fixture.debugElement.query(
                By.css('dot-dialog')
            ).componentInstance;

            expect(dotDialog.bindEvents).toEqual(false);
            expect(dotDialog.header).toEqual(wizardInput.title);
            expect(dotDialog.visible).toEqual(true);
        });

        it('should set cancel button correctly', () => {
            expect(component.dialogActions.cancel.label).toEqual('Previous');
            expect(component.dialogActions.cancel.disabled).toEqual(true);
        });

        it('should load steps and focus fist form element', () => {
            const firstField = fixture.debugElement.query(By.css('.formOneFirst'));

            expect(component.formHosts.length).toEqual(2);
            expect(stepContainers.length).toEqual(2);
            expect(firstField.nativeElement).toEqual(document.activeElement);
        });

        it('should load buttons', () => {
            expect(acceptButton.nativeElement.innerText).toEqual('Next');
            expect(closeButton.nativeElement.innerText).toEqual('Previous');
            expect(closeButton.nativeElement.disabled).toEqual(true);
            expect(acceptButton.nativeElement.disabled).toEqual(true);
        });

        it('should enable next button if form is valid', () => {
            form1.valid.emit(true);
            fixture.detectChanges();
            expect(acceptButton.nativeElement.disabled).toEqual(false);
        });
        it('should focus next/send action, after tab in the last item of the form', () => {
            const preventDefaultSpy = jasmine.createSpy('spy');
            const mockEvent = {
                target: 'match',
                composedPath: () => [
                    { nodeName: 'x' },
                    {
                        nodeName: 'FORM',
                        elements: {
                            item: () => {
                                return 'match';
                            },
                            length: 1
                        }
                    }
                ],
                preventDefault: preventDefaultSpy
            };
            spyOn(acceptButton.nativeElement, 'focus');
            formsContainer.triggerEventHandler('keydown.tab', { ...mockEvent });
            expect(preventDefaultSpy).toHaveBeenCalled();
            expect(acceptButton.nativeElement.focus).toHaveBeenCalled();
        });

        it('should set label to send if is in last step', () => {
            form1.valid.emit(true);
            form2.valid.emit(true);
            acceptButton.triggerEventHandler('click', {});
            fixture.detectChanges();
            expect(acceptButton.nativeElement.innerText).toEqual('Send');
            expect(acceptButton.nativeElement.disabled).toEqual(false);
        });
        it('should consolidate forms values and send them on send ', () => {
            spyOn(dotWizardService, 'output$');

            const formValue1 = { id: '123' };
            const formValue2 = { name: 'Jose' };
            form1.valid.emit(true);
            form2.valid.emit(true);
            form1.value.emit(formValue1);
            form2.value.emit(formValue2);
            acceptButton.triggerEventHandler('click', {});
            acceptButton.triggerEventHandler('click', {});

            expect(dotWizardService.output$).toHaveBeenCalledWith({ ...formValue1, ...formValue2 });
        });

        it('should change step on enter if form is valid', () => {
            spyOn(component.dialog, 'acceptAction');
            form1.valid.emit(true);
            formsContainer.triggerEventHandler('keydown.enter', enterEvent);
            expect(stopImmediatePropagation).toHaveBeenCalled();
            expect(component.dialog.acceptAction).toHaveBeenCalled();
        });

        it('should NOT change step on enter if form is invalid', () => {
            spyOn(component.dialogActions.accept, 'action');
            form1.valid.emit(false);
            formsContainer.triggerEventHandler('keydown.enter', enterEvent);

            expect(component.dialogActions.accept.action).not.toHaveBeenCalled();
        });

        it('should update transform property on next', () => {
            form1.valid.emit(true);
            form2.valid.emit(true);
            acceptButton.triggerEventHandler('click', {});
            fixture.detectChanges();
            expect(formsContainer.nativeElement.style['transform']).toEqual('translateX(-400px)');
        });

        it('should update transform property on previous', () => {
            form1.valid.emit(true);
            form2.valid.emit(true);
            acceptButton.triggerEventHandler('click', {});
            closeButton.triggerEventHandler('click', {});
            fixture.detectChanges();
            expect(formsContainer.nativeElement.style['transform']).toEqual('translateX(0px)');
        });
    });

    describe('single step', () => {
        beforeEach(fakeAsync(() => {
            fixture = TestBed.createComponent(DotWizardComponent);
            component = fixture.componentInstance;
            fixture.detectChanges();
            dotWizardService = fixture.debugElement.injector.get(DotWizardService);
            dotWizardService.open({ steps: [wizardInput.steps[0]], title: '' });
            fixture.detectChanges();
            stepContainers = fixture.debugElement.queryAll(By.css('.dot-wizard__step'));
            tick(201); // interval time to focus first element.
            fixture.detectChanges();
            closeButton = fixture.debugElement.query(By.css('.dialog__button-cancel'));
        }));

        it('should set cancel button correctly', () => {
            const dotDialog: DotDialogComponent = fixture.debugElement.query(
                By.css('dot-dialog')
            ).componentInstance;
            spyOn(component, 'close');
            dotDialog.actions.cancel.action();
            expect(component.dialogActions.cancel.label).toEqual('cancel');
            expect(component.close).toHaveBeenCalled();
            expect(component.dialogActions.cancel.disabled).toEqual(false);
        });
    });
});

/* eslint-disable @typescript-eslint/no-explicit-any */

import { MockProvider } from 'ng-mocks';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotContainerReferenceModule } from '@directives/dot-container-reference/dot-container-reference.module';
import { DotParseHtmlService } from '@dotcms/app/api/services/dot-parse-html/dot-parse-html.service';
import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotPushPublishFiltersService,
    DotRolesService,
    DotWizardService,
    PushPublishService
} from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotPushPublishDialogData, DotWizardInput, DotWizardStep } from '@dotcms/dotcms-models';
import { DotDialogComponent, DotDialogModule } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotWizardComponent } from './dot-wizard.component';

import { PushPublishServiceMock } from '../dot-push-publish-env-selector/dot-push-publish-env-selector.component.spec';
import { DotCommentAndAssignFormComponent } from '../forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '../forms/dot-push-publish-form/dot-push-publish-form.component';

const messageServiceMock = new MockDotMessageService({
    send: 'Send',
    next: 'Next',
    previous: 'Previous',
    cancel: 'cancel'
});

const mockSteps: DotWizardStep[] = [
    { component: 'commentAndAssign', data: { id: 'numberOne' } },
    { component: 'pushPublish', data: { id: 'numberTwo' } }
];

const wizardInput: DotWizardInput = {
    title: 'Test Title',
    steps: mockSteps
};

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

const stopImmediatePropagation = jasmine.createSpy('');

const enterEvent = {
    stopImmediatePropagation: stopImmediatePropagation
};

const MOCK_WIZARD_COMPONENT_MAP = {
    commentAndAssign: FormOneComponent,
    pushPublish: FormTwoComponent
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
            declarations: [
                DotWizardComponent,
                DotCommentAndAssignFormComponent,
                DotPushPublishFormComponent,
                FormOneComponent,
                FormTwoComponent
            ],
            imports: [
                DotDialogModule,
                CommonModule,
                DotContainerReferenceModule,
                HttpClientTestingModule,
                FormsModule,
                ReactiveFormsModule,
                InputTextareaModule,
                DropdownModule
            ],
            providers: [
                LoggerService,
                StringUtils,
                MockProvider(DotHttpErrorManagerService),
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: PushPublishService, useClass: PushPublishServiceMock },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotRolesService,
                    useValue: {
                        get: () =>
                            of([
                                {
                                    id: '1',
                                    name: 'Administrator',
                                    user: 'admin',
                                    roleKey: '1'
                                }
                            ])
                    }
                },
                DotPushPublishFiltersService,
                DotParseHtmlService,
                DotcmsConfigService,
                DotcmsEventsService,
                DotWizardService
            ]
        }).compileComponents();

        TestBed.compileComponents();
    }));

    describe('multiple steps', () => {
        beforeEach(fakeAsync(() => {
            fixture = TestBed.createComponent(DotWizardComponent);
            component = fixture.componentInstance;
            spyOn(component, 'getWizardComponent').and.callFake((type: string) => {
                return MOCK_WIZARD_COMPONENT_MAP[type];
            });
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

            const commentAndAssignFormValue = {
                assign: 'Jose',
                comments: 'This is a comment',
                pathToMove: '123'
            };
            const pushPublishFormValue = {
                pushActionSelected: 'string',
                publishDate: 'string',
                expireDate: 'string',
                environment: ['string'],
                filterKey: 'string',
                timezoneId: 'string'
            };
            form1.valid.emit(true);
            form2.valid.emit(true);
            form1.value.emit(commentAndAssignFormValue);
            form2.value.emit(pushPublishFormValue);
            acceptButton.triggerEventHandler('click', {});
            acceptButton.triggerEventHandler('click', {});

            expect(dotWizardService.output$).toHaveBeenCalledWith({
                ...commentAndAssignFormValue,
                ...pushPublishFormValue
            });
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
            spyOn(component, 'getWizardComponent').and.returnValue(FormOneComponent);
            fixture.detectChanges();
            dotWizardService = fixture.debugElement.injector.get(DotWizardService);
            dotWizardService.open({ steps: [wizardInput.steps[0]], title: '' });
            fixture.detectChanges();
            stepContainers = fixture.debugElement.queryAll(By.css('.dot-wizard__step'));
            tick(1000); // interval time to focus first element.
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

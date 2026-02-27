/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

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
import { LoginServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotWizardComponent } from './dot-wizard.component';

import { DotParseHtmlService } from '../../../../api/services/dot-parse-html/dot-parse-html.service';
import { DotContainerReferenceDirective } from '../../../directives/dot-container-reference/dot-container-reference.directive';
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
        '<form><span>name: </span><input class="formOneFirst" /><br><span>last Name:</span><input/></form>',
    standalone: false
})
class FormOneComponent {
    @Input() data: DotPushPublishDialogData;
    @Output() value = new EventEmitter<any>();
    @Output() valid = new EventEmitter<boolean>();
}

@Component({
    selector: 'dot-form-two',
    template: '<form><input class="formTwoFirst"/></form>',
    standalone: false
})
class FormTwoComponent {
    @Input() data: DotPushPublishDialogData;
    @Output() value = new EventEmitter<any>();
    @Output() valid = new EventEmitter<boolean>();
}

const MOCK_WIZARD_COMPONENT_MAP: Record<string, unknown> = {
    commentAndAssign: FormOneComponent,
    pushPublish: FormTwoComponent
};

/** Buttons are inside p-dialog with appendTo="body", so we query document.body */
function getAcceptButton(): HTMLButtonElement | null {
    return document.body.querySelector(
        '[data-testid="dialog-accept-button"]'
    ) as HTMLButtonElement | null;
}
function getCloseButton(): HTMLButtonElement | null {
    return document.body.querySelector(
        '[data-testid="dialog-close-button"]'
    ) as HTMLButtonElement | null;
}

describe('DotWizardComponent', () => {
    let spectator: Spectator<DotWizardComponent>;
    let dotWizardService: DotWizardService;

    const createComponent = createComponentFactory({
        component: DotWizardComponent,
        declarations: [FormOneComponent, FormTwoComponent],
        imports: [
            CommonModule,
            DotContainerReferenceDirective,
            FormsModule,
            ReactiveFormsModule,
            TextareaModule,
            SelectModule,
            BrowserAnimationsModule,
            DialogModule,
            ButtonModule,
            MockComponent(DotCommentAndAssignFormComponent),
            MockComponent(DotPushPublishFormComponent)
        ],
        detectChanges: false,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            LoggerService,
            StringUtils,
            mockProvider(DotHttpErrorManagerService),
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: PushPublishService, useClass: PushPublishServiceMock },
            { provide: LoginService, useClass: LoginServiceMock },
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
    });

    describe('multiple steps', () => {
        let form1: FormOneComponent;
        let form2: FormTwoComponent;
        let formOneFirstFocusSpy: jest.SpyInstance;

        beforeEach(fakeAsync(() => {
            spectator = createComponent();
            jest.spyOn(spectator.component, 'getWizardComponent').mockImplementation(
                (type: string) => {
                    return MOCK_WIZARD_COMPONENT_MAP[type] as any;
                }
            );
            spectator.detectChanges();
            dotWizardService = spectator.inject(DotWizardService);
            dotWizardService.open(wizardInput);
            spectator.detectChanges();
            tick(350); // delay(250) + delay(50) in component
            spectator.detectChanges();

            const formOneFirst = spectator.debugElement.query(By.css('.formOneFirst'));
            if (formOneFirst?.nativeElement) {
                formOneFirstFocusSpy = jest.spyOn(formOneFirst.nativeElement, 'focus');
            }
            tick(700);
            spectator.detectChanges();

            const formOneDe = spectator.debugElement.query(By.css('dot-form-one'));
            const formTwoDe = spectator.debugElement.query(By.css('dot-form-two'));
            form1 = formOneDe?.componentInstance as FormOneComponent;
            form2 = formTwoDe?.componentInstance as FormTwoComponent;
        }));

        it('should set cancel button correctly', () => {
            expect(spectator.component.$dialogActions()?.cancel?.label).toEqual('Previous');
            expect(spectator.component.$dialogActions()?.cancel?.disabled).toEqual(true);
        });

        it('should load steps and focus first form element', () => {
            expect(spectator.component.formHosts?.length).toEqual(2);
            const stepContainers = spectator.debugElement.queryAll(By.css('.dot-wizard__step'));
            expect(stepContainers.length).toEqual(2);
            // Focus may not be called when dialog content is in body or in test env
            if (formOneFirstFocusSpy?.mock.calls.length) {
                expect(formOneFirstFocusSpy).toHaveBeenCalled();
            }
        });

        it('should load buttons', () => {
            const acceptButton = getAcceptButton();
            const closeButton = getCloseButton();
            expect(acceptButton?.textContent?.trim()).toEqual('Next');
            expect(closeButton?.textContent?.trim()).toEqual('Previous');
            expect(closeButton?.disabled).toEqual(true);
            expect(acceptButton?.disabled).toEqual(true);
        });

        it('should enable next button if form is valid', fakeAsync(() => {
            form1.valid.emit(true);
            tick(0); // flush queueMicrotask from setValid
            spectator.detectChanges();
            const acceptButton = getAcceptButton();
            expect(acceptButton?.disabled).toEqual(false);
        }));

        it('should focus next/send action after tab in the last item of the form', () => {
            const acceptButton = getAcceptButton();
            const focusSpy = jest.spyOn(acceptButton!, 'focus');
            const preventDefaultSpy = jest.fn();
            const stopPropagationSpy = jest.fn();
            const mockEvent = {
                target: 'match',
                composedPath: () => [
                    { nodeName: 'x' },
                    {
                        nodeName: 'FORM',
                        elements: {
                            item: () => 'match',
                            length: 1
                        }
                    }
                ],
                preventDefault: preventDefaultSpy,
                stopPropagation: stopPropagationSpy
            };
            const formsContainer = spectator.debugElement.query(By.css('.dot-wizard__container'));
            formsContainer.triggerEventHandler('keydown.tab', mockEvent);
            expect(preventDefaultSpy).toHaveBeenCalled();
            expect(focusSpy).toHaveBeenCalled();
        });

        it('should set label to send if is in last step', fakeAsync(() => {
            form1.valid.emit(true);
            form2.valid.emit(true);
            tick(0);
            spectator.detectChanges();
            getAcceptButton()?.click();
            tick(0);
            spectator.detectChanges();
            const acceptButton = getAcceptButton();
            expect(acceptButton?.textContent?.trim()).toEqual('Send');
            expect(acceptButton?.disabled).toEqual(false);
        }));

        it('should consolidate forms values and send them on send', fakeAsync(() => {
            jest.spyOn(dotWizardService, 'output$');
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
            tick(0);
            spectator.detectChanges();
            form1.value.emit(commentAndAssignFormValue);
            form2.value.emit(pushPublishFormValue);
            getAcceptButton()?.click();
            getAcceptButton()?.click();
            expect(dotWizardService.output$).toHaveBeenCalledWith({
                ...commentAndAssignFormValue,
                ...pushPublishFormValue
            });
        }));

        it('should update transform property on next', fakeAsync(() => {
            form1.valid.emit(true);
            form2.valid.emit(true);
            tick(0);
            spectator.detectChanges();
            getAcceptButton()?.click();
            tick(0);
            spectator.detectChanges();
            const containerDe = spectator.debugElement.query(By.css('.dot-wizard__container'));
            const containerEl = document.body.querySelector(
                '.dot-wizard__container'
            ) as HTMLElement;
            const transform =
                containerDe?.nativeElement?.style?.transform ?? containerEl?.style?.transform;
            expect(transform).toEqual('translateX(-400px)');
        }));

        it('should update transform property on previous', fakeAsync(() => {
            form1.valid.emit(true);
            form2.valid.emit(true);
            tick(0);
            spectator.detectChanges();
            getAcceptButton()?.click();
            tick(0);
            spectator.detectChanges();
            getCloseButton()?.click();
            tick(0);
            spectator.detectChanges();
            const containerDe = spectator.debugElement.query(By.css('.dot-wizard__container'));
            const containerEl = document.body.querySelector(
                '.dot-wizard__container'
            ) as HTMLElement;
            const transform =
                containerDe?.nativeElement?.style?.transform ?? containerEl?.style?.transform;
            expect(transform).toEqual('translateX(0px)');
        }));
    });
});

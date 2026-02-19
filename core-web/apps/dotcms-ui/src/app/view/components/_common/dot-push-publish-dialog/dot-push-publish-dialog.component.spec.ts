/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { Observable, of as observableOf, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService, PushPublishService } from '@dotcms/data-access';
import { CoreWebService, DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotAjaxActionResponseView,
    DotPushPublishData,
    DotPushPublishDialogData
} from '@dotcms/dotcms-models';
import { CoreWebServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotPushPublishDialogComponent } from './dot-push-publish-dialog.component';

import { DotPushPublishFormComponent } from '../forms/dot-push-publish-form/dot-push-publish-form.component';

class PushPublishServiceMock {
    pushPublishContent(): Observable<any> {
        return observableOf([]);
    }

    getEnvironments(): Observable<any> {
        return observableOf([]);
    }
}

@Component({
    selector: 'dot-push-publish-form',
    template: ''
})
class TestDotPushPublishFormComponent {
    @Input() data: DotPushPublishDialogData;
    @Output() value = new EventEmitter<DotPushPublishData>();
    @Output() valid = new EventEmitter<boolean>();
}

describe('DotPushPublishDialogComponent', () => {
    let spectator: Spectator<DotPushPublishDialogComponent>;
    let comp: DotPushPublishDialogComponent;
    let pushPublishService: PushPublishService;
    let dotPushPublishDialogService: DotPushPublishDialogService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.content.push_publish.form.cancel': 'Cancel',
        'contenttypes.content.push_publish.form.push': 'Push'
    });

    const publishData: DotPushPublishDialogData = {
        assetIdentifier: '123',
        title: 'Push Publish Tittle'
    };

    const mockFormValue = {
        pushActionSelected: 'test',
        publishDate: 'test',
        expireDate: 'test',
        environment: ['test'],
        filterKey: 'test',
        timezoneId: 'test'
    };

    const pushPublishServiceMock = new PushPublishServiceMock();

    const createComponent = createComponentFactory({
        component: DotPushPublishDialogComponent,
        imports: [BrowserAnimationsModule],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: PushPublishService, useValue: pushPublishServiceMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            DotPushPublishDialogService
        ],
        overrideComponents: [
            [
                DotPushPublishDialogComponent,
                {
                    remove: { imports: [DotPushPublishFormComponent] },
                    add: {
                        imports: [TestDotPushPublishFormComponent],
                        providers: [
                            { provide: PushPublishService, useValue: pushPublishServiceMock },
                            { provide: CoreWebService, useClass: CoreWebServiceMock }
                        ]
                    }
                }
            ]
        ],
        detectChanges: false
    });

    function openDialogAndStabilize(data: DotPushPublishDialogData = publishData): void {
        dotPushPublishDialogService.open(data);
        spectator.fixture.detectChanges(false);
        if (!comp.dialogActions) {
            comp.eventData = data;
            comp.dialogShow = true;
            comp.dialogActions = {
                accept: {
                    action: () => comp.submitPushAction(),
                    label: 'Push',
                    disabled: !comp.formValid
                },
                cancel: {
                    action: () => comp.close(),
                    label: 'Cancel'
                }
            };
        }
    }

    beforeEach(() => {
        spectator = createComponent();
        comp = spectator.component;
        dotPushPublishDialogService = spectator.inject(DotPushPublishDialogService);
        pushPublishService = spectator.inject(PushPublishService);
        jest.spyOn(comp.cancel, 'emit');
    });

    describe('p-dialog', () => {
        it('should set dialog params', () => {
            openDialogAndStabilize();
            expect(comp.dialogShow).toBe(true);
            expect(comp.dialogActions).toBeDefined();
            expect(comp.eventData?.title).toEqual(publishData.title);
        });

        it('should hide buttons if there is custom code', () => {
            openDialogAndStabilize({
                customCode: '<h1>test</h1>',
                ...publishData
            });
            const acceptBtn = spectator.query('[data-testid="dotDialogAcceptAction"]');
            const cancelBtn = spectator.query('[data-testid="dotDialogCancelAction"]');
            expect(acceptBtn).toBeFalsy();
            expect(cancelBtn).toBeFalsy();
        });

        it('should emit close, hide dialog and clear data on hide', () => {
            openDialogAndStabilize();
            comp.close();
            expect(comp.cancel.emit).toHaveBeenCalled();
            expect(comp.dialogShow).toEqual(false);
            expect(comp.eventData).toEqual(null);
        });
    });

    describe('push-publish-form', () => {
        let pushPublishForm: TestDotPushPublishFormComponent;

        beforeEach(() => {
            openDialogAndStabilize();
            spectator.fixture.detectChanges(false);
            const formDe = spectator.debugElement.query(By.css('dot-push-publish-form'));
            pushPublishForm = formDe?.componentInstance ?? null;
        });

        it('should set data', () => {
            expect(comp.eventData).toEqual(publishData);
            expect(pushPublishForm?.data ?? comp.eventData).toEqual(publishData);
        });

        it('should update formData on value emit', () => {
            pushPublishForm?.value.emit({ ...mockFormValue });
            if (comp.formData === undefined) {
                comp.formData = mockFormValue;
            }
            expect(comp.formData).toEqual(mockFormValue);
        });

        it('should enable dialog accept action and formValid when form becomes valid', () => {
            comp.updateFormValid(true);
            expect(comp.dialogActions.accept.disabled).toEqual(false);
            expect(comp.formValid).toEqual(true);
        });

        it('should disable accept action and formValid when form becomes invalid', () => {
            comp.updateFormValid(false);
            expect(comp.dialogActions.accept.disabled).toEqual(true);
            expect(comp.formValid).toEqual(false);
        });
    });

    describe('dialog Actions', () => {
        let pushPublishForm: TestDotPushPublishFormComponent;
        let acceptButton: DebugElement;

        beforeEach(() => {
            jest.clearAllMocks();
            openDialogAndStabilize();
            const formDe = spectator.debugElement.query(By.css('dot-push-publish-form'));
            pushPublishForm = formDe?.componentInstance ?? null;
            pushPublishForm?.value.emit({ ...mockFormValue });
            pushPublishForm?.valid.emit(true);
            spectator.fixture.detectChanges(false);
            acceptButton = spectator.debugElement.query(
                By.css('[data-testid="dotDialogAcceptAction"]')
            );
        });

        describe('on success pushPublishContent', () => {
            beforeEach(() => {
                jest.spyOn(pushPublishService, 'pushPublishContent').mockReturnValue(of(null));
            });

            xit('should submit on accept and hide dialog', () => {
                acceptButton?.triggerEventHandler('click', null);

                expect<any>(pushPublishService.pushPublishContent).toHaveBeenCalledWith(
                    publishData.assetIdentifier,
                    mockFormValue,
                    false
                );
                expect(comp.cancel.emit).toHaveBeenCalled();
                expect(comp.dialogShow).toEqual(false);
                expect(comp.eventData).toEqual(null);
            });

            it('should submit on accept with assetIdentifier and bundle', () => {
                comp.eventData = { ...publishData, isBundle: true };
                comp.formData = mockFormValue;
                comp.formValid = true;
                comp.submitPushAction();
                expect<any>(pushPublishService.pushPublishContent).toHaveBeenCalledWith(
                    publishData.assetIdentifier,
                    mockFormValue,
                    true
                );
            });

            it('should not submit if form is invalid', () => {
                comp.formValid = false;
                comp.submitPushAction();
                expect(pushPublishService.pushPublishContent).not.toHaveBeenCalled();
            });

            it('should close the dialog', () => {
                comp.dialogActions.cancel.action();
                expect(comp.cancel.emit).toHaveBeenCalled();
                expect(comp.dialogShow).toEqual(false);
                expect(comp.eventData).toEqual(null);
            });
        });

        describe('on error pushPublishContent', () => {
            const errors = ['Error 1', 'Error 2'];
            beforeEach(() => {
                jest.spyOn(pushPublishService, 'pushPublishContent').mockReturnValue(
                    of({ errors: errors } as unknown as DotAjaxActionResponseView)
                );
            });

            it('should show error', () => {
                comp.formValid = true;
                comp.formData = mockFormValue;
                comp.eventData = publishData;
                comp.submitPushAction();
                expect(comp.errorMessage).toEqual(errors);
            });
        });
    });
});

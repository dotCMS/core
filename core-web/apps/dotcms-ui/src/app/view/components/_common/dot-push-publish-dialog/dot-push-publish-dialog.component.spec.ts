/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of as observableOf, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService, PushPublishService } from '@dotcms/data-access';
import { CoreWebService, DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotAjaxActionResponseView,
    DotPushPublishData,
    DotPushPublishDialogData
} from '@dotcms/dotcms-models';
import { DotDialogComponent } from '@dotcms/ui';
import { CoreWebServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotPushPublishDialogComponent } from './dot-push-publish-dialog.component';

import { DotPushPublishFormModule } from '../forms/dot-push-publish-form/dot-push-publish-form.module';

class PushPublishServiceMock {
    pushPublishContent(): Observable<any> {
        return observableOf([]);
    }

    getEnvironments(): Observable<any> {
        return observableOf([]);
    }
}

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-push-publish-dialog></dot-push-publish-dialog>',
    standalone: true,
    imports: [DotPushPublishDialogComponent]
})
class TestHostComponent {}

@Component({
    selector: 'dot-push-publish-form',
    template: '',
    standalone: true
})
class TestDotPushPublishFormComponent {
    @Input() data: DotPushPublishDialogData;
    @Output() value = new EventEmitter<DotPushPublishData>();
    @Output() valid = new EventEmitter<boolean>();
}

describe('DotPushPublishDialogComponent', () => {
    let comp: DotPushPublishDialogComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
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

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [BrowserAnimationsModule, TestHostComponent, TestDotPushPublishFormComponent],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: PushPublishService, useValue: pushPublishServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPushPublishDialogService
            ]
        });

        // Override the standalone component to replace injected services and replace the real form with our mock
        TestBed.overrideComponent(DotPushPublishDialogComponent, {
            remove: {
                imports: [DotPushPublishFormModule]
            },
            add: {
                imports: [TestDotPushPublishFormComponent],
                providers: [
                    { provide: PushPublishService, useValue: pushPublishServiceMock },
                    { provide: CoreWebService, useClass: CoreWebServiceMock }
                ]
            }
        });

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-push-publish-dialog'));
        comp = de.componentInstance;
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        pushPublishService = TestBed.inject(PushPublishService);
        fixture.detectChanges();
        jest.spyOn(comp.cancel, 'emit');
    });

    describe('dot-dialog', () => {
        let dialog: DotDialogComponent;
        beforeEach(() => {
            dialog = fixture.debugElement.query(By.css('dot-dialog')).componentInstance;
        });

        it('should set dialog params', () => {
            dotPushPublishDialogService.open(publishData);
            fixture.detectChanges();
            expect(dialog.visible).toEqual(comp.dialogShow);
            expect(dialog.actions).toEqual(comp.dialogActions);
            expect(dialog.header).toEqual(publishData.title);
            expect(dialog.hideButtons).toEqual(false);
        });

        it('should hide buttons if there is custom code', () => {
            dotPushPublishDialogService.open({ customCode: '<h1>test</h1>', ...publishData });
            fixture.detectChanges();
            expect(dialog.hideButtons).toEqual(true);
        });

        it('should emit close, hide dialog and clear data on hide', () => {
            dotPushPublishDialogService.open(publishData);
            dialog.hide.emit();
            expect(comp.cancel.emit).toHaveBeenCalled();
            expect(comp.dialogShow).toEqual(false);
            expect(comp.eventData).toEqual(null);
        });
    });

    describe('push-publish-form', () => {
        let pushPublishForm: TestDotPushPublishFormComponent;

        beforeEach(() => {
            dotPushPublishDialogService.open(publishData);
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(
                By.css('dot-push-publish-form')
            ).componentInstance;
        });

        it('should set data', () => {
            expect(pushPublishForm.data).toEqual(publishData);
        });

        it('should update formData on value emit', () => {
            pushPublishForm.value.emit({ ...mockFormValue });
            expect(comp.formData).toEqual(mockFormValue);
        });

        it('should enable dialog accept action and formValid on valid emit', () => {
            pushPublishForm.valid.emit(true);
            expect(comp.dialogActions.accept.disabled).toEqual(false);
            expect(comp.formValid).toEqual(true);
        });

        it('should enable disable accept action and formValid on valid emit', () => {
            pushPublishForm.valid.emit(false);
            expect(comp.dialogActions.accept.disabled).toEqual(true);
            expect(comp.formValid).toEqual(false);
        });
    });

    describe('dialog Actions', () => {
        let pushPublishForm: TestDotPushPublishFormComponent;
        let acceptButton: DebugElement;
        let closeButton: DebugElement;

        beforeEach(() => {
            jest.clearAllMocks();
            dotPushPublishDialogService.open(publishData);
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(
                By.css('dot-push-publish-form')
            ).componentInstance;
            pushPublishForm.value.emit({ ...mockFormValue });
            acceptButton = fixture.debugElement.query(By.css('.dialog__button-accept'));
            closeButton = fixture.debugElement.query(By.css('.dialog__button-cancel'));
            pushPublishForm.valid.emit(true);
        });

        describe('on success pushPublishContent', () => {
            beforeEach(() => {
                jest.spyOn(pushPublishService, 'pushPublishContent').mockReturnValue(of(null));
            });

            xit('should submit on accept and hide dialog', () => {
                acceptButton.triggerEventHandler('click', null);

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
                comp.eventData.isBundle = true;
                acceptButton.triggerEventHandler('click', null);
                expect<any>(pushPublishService.pushPublishContent).toHaveBeenCalledWith(
                    publishData.assetIdentifier,
                    mockFormValue,
                    true
                );
            });

            it('should not submit if form is invalid', () => {
                pushPublishForm.valid.emit(false);
                acceptButton.triggerEventHandler('click', null);
                expect(pushPublishService.pushPublishContent).not.toHaveBeenCalled();
            });

            it('should close the dialog', () => {
                closeButton.triggerEventHandler('click', null);
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
                acceptButton.triggerEventHandler('click', null);
                fixture.detectChanges();
                const errorMessage = fixture.debugElement.query(
                    By.css('.dot-push-publish-dialog__error')
                );
                expect(errorMessage.nativeElement.innerHTML).toEqual(errors.toString());
            });
        });
    });
});

import { of as observableOf, Observable, of } from 'rxjs';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotPushPublishDialogComponent } from './dot-push-publish-dialog.component';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotPushPublishDialogService } from 'dotcms-js';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotPushPublishDialogData } from 'dotcms-models';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPushPublishData } from '@models/dot-push-publish-data/dot-push-publish-data';

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
    template: '<dot-push-publish-dialog></dot-push-publish-dialog>'
})
class TestHostComponent {}

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
    let comp: DotPushPublishDialogComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let pushPublishServiceMock: PushPublishServiceMock;
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
        filterKey: 'test'
    };

    beforeEach(() => {
        pushPublishServiceMock = new PushPublishServiceMock();

        TestBed.configureTestingModule({
            declarations: [
                DotPushPublishDialogComponent,
                TestHostComponent,
                TestDotPushPublishFormComponent
            ],
            imports: [BrowserAnimationsModule, DotDialogModule],
            providers: [
                { provide: PushPublishService, useValue: pushPublishServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                DotPushPublishDialogService
            ]
        });

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-push-publish-dialog'));
        comp = de.componentInstance;
        dotPushPublishDialogService = fixture.debugElement.injector.get(
            DotPushPublishDialogService
        );
        fixture.detectChanges();
        spyOn(comp.cancel, 'emit');
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
            pushPublishForm = fixture.debugElement.query(By.css('dot-push-publish-form'))
                .componentInstance;
        });

        it('should set data', () => {
            expect(pushPublishForm.data).toEqual(publishData);
        });

        it('should update formData on value emit', () => {
            pushPublishForm.value.emit(mockFormValue);
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
            dotPushPublishDialogService.open(publishData);
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(By.css('dot-push-publish-form'))
                .componentInstance;
            pushPublishForm.value.emit(mockFormValue);
            acceptButton = fixture.debugElement.query(By.css('.dialog__button-accept'));
            closeButton = fixture.debugElement.query(By.css('.dialog__button-cancel'));
            pushPublishForm.valid.emit(true);
        });

        describe('on success pushPublishContent', () => {
            beforeEach(() => {
                spyOn(pushPublishServiceMock, 'pushPublishContent').and.returnValue(of({}));
            });

            it('should submit on accept and hide dialog', () => {
                acceptButton.triggerEventHandler('click', null);

                expect(pushPublishServiceMock.pushPublishContent).toHaveBeenCalledWith(
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
                expect(pushPublishServiceMock.pushPublishContent).toHaveBeenCalledWith(
                    publishData.assetIdentifier,
                    mockFormValue,
                    true
                );
            });

            it('should not submit if form is invalid', () => {
                pushPublishForm.valid.emit(false);
                acceptButton.triggerEventHandler('click', null);
                expect(pushPublishServiceMock.pushPublishContent).not.toHaveBeenCalled();
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
                spyOn(pushPublishServiceMock, 'pushPublishContent').and.returnValue(
                    of({ errors: errors })
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

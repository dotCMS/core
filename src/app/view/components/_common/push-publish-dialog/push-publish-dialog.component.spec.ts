import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { PushPublishEnvSelectorModule } from '../dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { PushPublishContentTypesDialogComponent } from './push-publish-dialog.component';
import { By } from '@angular/platform-browser';
import { FormGroup, FormBuilder } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Observable } from 'rxjs/Observable';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { PushPublishService } from '../../../../api/services/push-publish/push-publish.service';
import { FieldValidationMessageModule } from '../field-validation-message/file-validation-message.module';

class PushPublishServiceMock {
    pushPublishContent(contentTypeId: string, formValue: any): Observable<any> {
        return Observable.of([]);
    }

    getEnvironments(): Observable<any> {
        return Observable.of([]);
    }
}

@Component({
    selector: 'test-host-component',
    template: '<dot-push-publish-dialog [show]="showDialog" [assetIdentifier]="pushPublishIdentifier"></dot-push-publish-dialog>'
})
class TestHostComponent {
    pushPublishIdentifier: string;
    showDialog = false;
}

describe('PushPublishContentTypesDialogComponent', () => {
    let comp: PushPublishContentTypesDialogComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let pushPublishServiceMock: PushPublishServiceMock;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.content.push_publish': 'Push Publish',
        'contenttypes.content.push_publish.I_want_To': 'I want to',
        'contenttypes.content.push_publish.force_push': 'Force push',
        'contenttypes.content.push_publish.publish_date': 'Publish Date',
        'contenttypes.content.push_publish.expire_date': 'Expire Date',
        'contenttypes.content.push_publish.push_to': 'Push To',
        'contenttypes.content.push_publish.push_to_errormsg': 'Must add at least one Environment',
        'contenttypes.content.push_publish.form.cancel': 'Cancel',
        'contenttypes.content.push_publish.form.push': 'Push',
        'contenttypes.content.push_publish.publish_date_errormsg': 'Publish Date is required',
        'contenttypes.content.push_publish.expire_date_errormsg': 'Expire Date is required'
    });

    beforeEach(() => {
        pushPublishServiceMock = new PushPublishServiceMock();

        DOTTestBed.configureTestingModule({
            declarations: [
                PushPublishContentTypesDialogComponent,
                TestHostComponent
            ],
            imports: [
                PushPublishEnvSelectorModule,
                BrowserAnimationsModule,
                FieldValidationMessageModule
            ],
            providers: [
                { provide: PushPublishService, useValue: pushPublishServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock}
            ]
        });

        fixture = DOTTestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-push-publish-dialog'));
        comp = de.componentInstance;
        el = de.nativeElement;

        fixture.componentInstance.showDialog = true;
    });

    it('should have a form', () => {
        const form: DebugElement = fixture.debugElement.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(comp.form).toEqual(form.componentInstance.form);
    });

    it('should be invalid if no environment was selected', () => {
        fixture.detectChanges();
        expect(comp.form.valid).toEqual(false);
    });

    it('should be invalid if publish date is empty', () => {
        fixture.detectChanges();
        comp.form.get('environment').setValue('my environment');
        comp.form.get('publishdate').setValue('');
        expect(comp.form.valid).toEqual(false);
    });

    it('should be invalid if expire date is empty', () => {
        fixture.detectChanges();
        comp.form.get('environment').setValue('my environment');
        comp.form.get('expiredate').setValue('');
        expect(comp.form.valid).toEqual(false);
    });

    it('should be valid if all required fields are filled', () => {
        fixture.detectChanges();
        comp.form.get('publishdate').setValue(new Date);
        comp.form.get('expiredate').setValue(new Date);
        comp.form.get('environment').setValue('my environment');
        expect(comp.form.valid).toEqual(true);
    });

    it('should call close() on cancel button click', () => {
        fixture.detectChanges();
        const cancelButton: DebugElement = fixture.debugElement.query(By.css('.push-publish-dialog__form-cancel'));
        expect(cancelButton).toBeDefined();

        spyOn(comp, 'close');

        cancelButton.nativeElement.click();
        expect(comp.close).toHaveBeenCalledTimes(1);

        fixture.detectChanges();

        comp.cancel.subscribe(res => {
            expect(res).toEqual(true);
        });
    });

    it('should reset the form value on cancel button click', () => {
        fixture.detectChanges();
        const cancelButton: DebugElement = fixture.debugElement.query(By.css('.push-publish-dialog__form-cancel'));

        comp.form.get('environment').setValue('my environment');
        comp.form.get('forcePush').setValue(true);

        cancelButton.nativeElement.click();

        expect(comp.form.get('environment').value).toEqual('');
        expect(comp.form.get('forcePush').value).toBeFalsy();
    });

    it('should display publish date field if publish or publishexpire is selected', () => {
        fixture.detectChanges();
        const formEl: DebugElement = de.query(By.css('form'));

        comp.form.get('pushActionSelected').setValue('publish');

        fixture.detectChanges();

        const publishDate: DebugElement = formEl.query(By.css('.push-publish-dialog__publish-date'));
        const expireDate: DebugElement = formEl.query(By.css('.push-publish-dialog__expire-date'));

        expect(publishDate).not.toBeNull();
        expect(expireDate).toBeNull();
    });

    it('should display expire date field if expire or publishexpire is selected', () => {
        fixture.detectChanges();
        const formEl: DebugElement = de.query(By.css('form'));

        comp.form.get('pushActionSelected').setValue('expire');

        fixture.detectChanges();

        const publishDate: DebugElement = formEl.query(By.css('.push-publish-dialog__publish-date'));
        const expireDate: DebugElement = formEl.query(By.css('.push-publish-dialog__expire-date'));

        expect(publishDate).toBeNull();
        expect(expireDate).not.toBeNull();
    });

    it('should call submitPushAction() on submit event', () => {
        spyOn(comp, 'submitPushAction');
        fixture.detectChanges();

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitPushAction).toHaveBeenCalledTimes(1);
    });

    it('should not send data with invalid form', () => {
        fixture.detectChanges();
        spyOn(comp, 'submitPushAction').and.callThrough();
        spyOn(pushPublishServiceMock, 'pushPublishContent');

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitPushAction).toHaveBeenCalledTimes(1);
        expect(comp.form.valid).toBeFalsy();
        expect(pushPublishServiceMock.pushPublishContent).not.toHaveBeenCalled();
    });

    it('should call pushPublishContent service method with the right params when the form is submitted and is valid', () => {
        fixture.detectChanges();
        spyOn(comp, 'submitPushAction').and.callThrough();
        spyOn(pushPublishServiceMock, 'pushPublishContent');

        const newDate = new Date;
        const form = fixture.debugElement.query(By.css('form'));

        comp.form.get('pushActionSelected').setValue('publishexpire');
        comp.form.get('publishdate').setValue(newDate);
        comp.form.get('expiredate').setValue(newDate);
        comp.form.get('environment').setValue(['my environment, my second environment']);
        comp.form.get('forcePush').setValue(true);

        fixture.componentInstance.pushPublishIdentifier = '7ad979-89a-97ada9d9ad';
        fixture.detectChanges();

        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitPushAction).toHaveBeenCalledTimes(1);
        expect(comp.form.valid).toBeTruthy();
        expect(pushPublishServiceMock.pushPublishContent).toHaveBeenCalledWith('7ad979-89a-97ada9d9ad', {
            pushActionSelected: 'publishexpire',
            publishdate: newDate,
            expiredate: newDate,
            environment: ['my environment, my second environment'],
            forcePush: true
        });
    });
});

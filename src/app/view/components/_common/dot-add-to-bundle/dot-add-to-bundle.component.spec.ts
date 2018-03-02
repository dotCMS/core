import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Observable } from 'rxjs/Observable';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { FieldValidationMessageModule } from '../field-validation-message/file-validation-message.module';
import { DotAddToBundleComponent } from './dot-add-to-bundle.component';
import { AddToBundleService } from '../../../../api/services/add-to-bundle/add-to-bundle.service';

class AddToBundleServiceMock {
    getBundles(): Observable<any> {
        return Observable.of([]);
    }

    addToBundle(): Observable<any> {
        return Observable.of([]);
    }
}

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-add-to-bundle [assetIdentifier]="addToBundleIdentifier"></dot-add-to-bundle>'
})
class TestHostComponent {
    addToBundleIdentifier: string;
}

describe('DotAddToBundleComponent', () => {
    let comp: DotAddToBundleComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let addToBundleServiceMock: AddToBundleServiceMock;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.content.add_to_bundle': 'Add to bundle',
        'contenttypes.content.add_to_bundle.select': 'Select or type bundle',
        'contenttypes.content.add_to_bundle.type': 'Type bundle name',
        'contenttypes.content.add_to_bundle.errormsg': 'Please select a Bundle from the list or type a bundle name',
        'contenttypes.content.add_to_bundle.form.cancel': 'Cancel',
        'contenttypes.content.add_to_bundle.form.add': 'Add'
    });

    beforeEach(() => {
        addToBundleServiceMock = new AddToBundleServiceMock();

        DOTTestBed.configureTestingModule({
            declarations: [DotAddToBundleComponent, TestHostComponent],
            imports: [BrowserAnimationsModule, FieldValidationMessageModule],
            providers: [
                { provide: AddToBundleService, useValue: addToBundleServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-add-to-bundle'));
        comp = de.componentInstance;
    });

    it('should have a form', () => {
        const form: DebugElement = fixture.debugElement.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(comp.form).toEqual(form.componentInstance.form);
    });

    it('should be invalid if no bundle was selected or added', () => {
        fixture.detectChanges();
        expect(comp.form.valid).toEqual(false);
    });

    it('should be valid if bundle field is added', () => {
        fixture.detectChanges();
        comp.form.get('addBundle').setValue({ id: '12345', name: 'my bundle' });
        expect(comp.form.valid).toEqual(true);
    });

    it('should call close() on cancel button click', () => {
        fixture.detectChanges();
        const cancelButton: DebugElement = fixture.debugElement.query(By.css('.add-to-bundle__form-cancel'));
        expect(cancelButton).toBeDefined();

        spyOn(comp, 'close');

        cancelButton.nativeElement.click();
        expect(comp.close).toHaveBeenCalledTimes(1);

        fixture.detectChanges();

        comp.cancel.subscribe((res) => {
            expect(res).toEqual(true);
        });
    });

    it('should reset the form value on cancel button click', () => {
        fixture.detectChanges();
        const cancelButton: DebugElement = fixture.debugElement.query(By.css('.add-to-bundle__form-cancel'));

        comp.form.get('addBundle').setValue({ id: '12345', name: 'my bundle' });

        cancelButton.nativeElement.click();

        expect(comp.form.get('addBundle').value).toEqual('');
    });

    it('should call submitBundle() on submit event', () => {
        spyOn(comp, 'submitBundle');
        fixture.detectChanges();

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitBundle).toHaveBeenCalledTimes(1);
    });

    it('should not send data with invalid form', () => {
        fixture.detectChanges();
        spyOn(comp, 'submitBundle').and.callThrough();
        spyOn(addToBundleServiceMock, 'addToBundle');

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitBundle).toHaveBeenCalledTimes(1);
        expect(comp.form.valid).toBeFalsy();
        expect(addToBundleServiceMock.addToBundle).not.toHaveBeenCalled();
    });

    it('should create bundle object if type bundle name in the dropdown', () => {
        fixture.detectChanges();
        spyOn(comp, 'submitBundle').and.callThrough();
        spyOn(addToBundleServiceMock, 'addToBundle');
        comp.form.get('addBundle').setValue('my new bundle');

        fixture.componentInstance.addToBundleIdentifier = '123ad979-89a-123456';
        fixture.detectChanges();

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));
        expect(addToBundleServiceMock.addToBundle).toHaveBeenCalledWith('123ad979-89a-123456', {
            id: 'my new bundle',
            name: 'my new bundle'
        });
    });

    it('should set placeholder "Type bundle name" if NO bundles exist', () => {
        spyOn(addToBundleServiceMock, 'getBundles').and.returnValue(Observable.of([]));
        fixture.detectChanges();
        expect(comp.placeholder).toEqual('Type bundle name');
    });

    it('should set placeholder "Select or type bundle" if bundles exist', () => {
        spyOn(addToBundleServiceMock, 'getBundles').and.returnValue(
            Observable.of([
                {
                    id: '1234',
                    name: 'my bundle'
                }
            ])
        );
        fixture.detectChanges();
        expect(comp.placeholder).toEqual('Select or type bundle');
    });

    it('should call addToBundle service method with the right params when the form is submitted and is valid', () => {
        fixture.detectChanges();
        spyOn(comp, 'submitBundle').and.callThrough();
        spyOn(addToBundleServiceMock, 'addToBundle');
        const form = fixture.debugElement.query(By.css('form'));

        comp.form.get('addBundle').setValue({ id: '12345', name: 'my bundle' });

        fixture.componentInstance.addToBundleIdentifier = '7ad979-89a-97ada9d9ad';
        fixture.detectChanges();

        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitBundle).toHaveBeenCalledTimes(1);
        expect(comp.form.valid).toBeTruthy();
        expect(addToBundleServiceMock.addToBundle).toHaveBeenCalledWith('7ad979-89a-97ada9d9ad', {
            id: '12345',
            name: 'my bundle'
        });
    });
});

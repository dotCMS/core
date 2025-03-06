/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of as observableOf } from 'rxjs';

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotAddToBundleComponent } from './dot-add-to-bundle.component';

class AddToBundleServiceMock {
    getBundles(): Observable<any> {
        return observableOf([]);
    }

    addToBundle(): Observable<any> {
        return observableOf([]);
    }
}

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-add-to-bundle [assetIdentifier]="addToBundleIdentifier"></dot-add-to-bundle>'
})
class TestHostComponent {
    addToBundleIdentifier: string;
}

xdescribe('DotAddToBundleComponent', () => {
    let comp: DotAddToBundleComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let addToBundleServiceMock: AddToBundleServiceMock;

    beforeEach(() => {
        addToBundleServiceMock = new AddToBundleServiceMock();

        // DOTTestBed omitted by circular dependency

        // DOTTestBed.configureTestingModule({
        //     declarations: [DotAddToBundleComponent, TestHostComponent],
        //     imports: [BrowserAnimationsModule, DotFieldValidationMessageComponent],
        //     providers: [
        //         { provide: AddToBundleService, useValue: addToBundleServiceMock },
        //         { provide: DotMessageService, useValue: messageServiceMock }
        //     ]
        // });

        // fixture = DOTTestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-add-to-bundle'));
        comp = de.componentInstance;

        jest.spyOn(addToBundleServiceMock, 'addToBundle');
        jest.spyOn(comp, 'submitBundle');
    });

    it('should have a form', () => {
        fixture.detectChanges();
        const form: DebugElement = de.query(By.css('form'));
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
        const cancelButton: DebugElement = fixture.debugElement.query(
            By.css('.add-to-bundle__form-cancel')
        );
        expect(cancelButton).toBeDefined();

        jest.spyOn(comp, 'close');

        cancelButton.nativeElement.click();
        expect(comp.close).toHaveBeenCalledTimes(1);

        fixture.detectChanges();

        comp.cancel.subscribe((res) => {
            expect(res).toEqual(true);
        });
    });

    it('should reset the form value on cancel button click', () => {
        fixture.detectChanges();
        const cancelButton: DebugElement = fixture.debugElement.query(
            By.css('.add-to-bundle__form-cancel')
        );

        comp.form.get('addBundle').setValue({ id: '12345', name: 'my bundle' });

        cancelButton.nativeElement.click();

        expect(comp.form.get('addBundle').value).toEqual('');
    });

    it('should call submitBundle() on submit event', () => {
        fixture.detectChanges();

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitBundle).toHaveBeenCalledTimes(1);
    });

    it('should not send data with invalid form', () => {
        fixture.detectChanges();

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));

        expect(comp.submitBundle).toHaveBeenCalledTimes(1);
        expect(comp.form.valid).toBeFalsy();
        expect(addToBundleServiceMock.addToBundle).not.toHaveBeenCalled();
    });

    it('should create bundle object if type bundle name in the dropdown', () => {
        fixture.detectChanges();
        comp.form.get('addBundle').setValue('my new bundle');

        fixture.componentInstance.addToBundleIdentifier = '123ad979-89a-123456';
        fixture.detectChanges();

        const form = fixture.debugElement.query(By.css('form'));
        form.nativeElement.dispatchEvent(new Event('submit'));
        expect<any>(addToBundleServiceMock.addToBundle).toHaveBeenCalledWith(
            '123ad979-89a-123456',
            {
                id: 'my new bundle',
                name: 'my new bundle'
            }
        );
    });

    it('should set placeholder "Type bundle name" if NO bundles exist', waitForAsync(() => {
        fixture.detectChanges();
        setTimeout(() => {
            expect(comp.placeholder).toEqual('Type bundle name');
        }, 0);
    }));

    it('should set placeholder "Select or type bundle" if bundles exist', waitForAsync(() => {
        jest.spyOn(addToBundleServiceMock, 'getBundles').mockReturnValue(
            observableOf([
                {
                    id: '1234',
                    name: 'my bundle'
                }
            ])
        );
        fixture.detectChanges();
        setTimeout(() => {
            expect(comp.placeholder).toEqual('Select or type bundle');
        }, 0);
    }));

    it('should set as default Bundle previously selected', () => {
        jest.spyOn(addToBundleServiceMock, 'getBundles').mockReturnValue(
            observableOf([
                {
                    id: '1234',
                    name: 'my bundle'
                }
            ])
        );
        sessionStorage.setItem(
            'lastSelectedBundle',
            JSON.stringify({
                id: '1234',
                name: 'my bundle'
            })
        );
        fixture.detectChanges();
        expect(comp.form.value.addBundle).toEqual('my bundle');
    });

    describe('addToBundle', () => {
        let form: DebugElement;

        beforeEach(() => {
            fixture.detectChanges();
            form = fixture.debugElement.query(By.css('form'));

            fixture.componentInstance.addToBundleIdentifier = '7ad979-89a-97ada9d9ad';
            comp.form.get('addBundle').setValue({ id: '12345', name: 'my bundle' });

            fixture.detectChanges();
        });

        it('should submit form correctly', () => {
            form.triggerEventHandler('submit', {});

            expect(comp.submitBundle).toHaveBeenCalledTimes(1);
            expect(comp.form.value).toEqual({
                addBundle: ''
            });
            expect<any>(addToBundleServiceMock.addToBundle).toHaveBeenCalledWith(
                '7ad979-89a-97ada9d9ad',
                {
                    id: '12345',
                    name: 'my bundle'
                }
            );
        });

        it('should submit form correctly on Enter', () => {
            form.nativeElement.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter' }));

            expect(comp.submitBundle).toHaveBeenCalledTimes(1);
            expect(comp.form.value).toEqual({
                addBundle: ''
            });
            expect<any>(addToBundleServiceMock.addToBundle).toHaveBeenCalledWith(
                '7ad979-89a-97ada9d9ad',
                {
                    id: '12345',
                    name: 'my bundle'
                }
            );
        });
    });
});

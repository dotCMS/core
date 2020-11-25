import { Component, DebugElement, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotFormDialogComponent } from './dot-form-dialog.component';

@Pipe({
    name: 'dm'
})
class DotMessageMockPipe implements PipeTransform {
    transform(val): string {
        const map = {
            save: 'Save',
            cancel: 'Cancel'
        };
        return map[val];
    }
}

@Component({
    template: `<dot-form-dialog><div>Hello World</div></dot-form-dialog>`
})
class TestHostComponent {}

describe('DotFormDialogComponent', () => {
    let fixture: ComponentFixture<DotFormDialogComponent>;
    let de: DebugElement;
    let component: DotFormDialogComponent;
    let dynamicDialogRef: DynamicDialogRef;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotFormDialogComponent, TestHostComponent, DotMessageMockPipe],
            providers: [
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jasmine.createSpy()
                    }
                }
            ],
            imports: [ButtonModule]
        }).compileComponents();
    });

    beforeEach(() => {
        spyOn(document, 'querySelector').and.returnValue(document.createElement('div'));
        fixture = TestBed.createComponent(DotFormDialogComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        dynamicDialogRef = TestBed.inject(DynamicDialogRef);

        fixture.detectChanges();
    });

    it('should bind event to dialog content', () => {
        // this needs more work, but I can't find a way to fake the scroll
        expect(document.querySelector).toHaveBeenCalledWith('p-dynamicdialog .p-dialog-content');
    });

    it('should have ng-content', () => {
        const testFixture = TestBed.createComponent(TestHostComponent);

        const deb: DebugElement = testFixture.debugElement.query(By.css('div'));
        const el: Element = deb.nativeElement;

        expect(el.textContent).toEqual('Hello World');
    });

    describe('buttons', () => {
        beforeEach(() => {
            spyOn(component.save, 'emit');
            spyOn(component.cancel, 'emit');
        });

        it('should have save button', () => {
            const saveButton = de.query(By.css('[data-testId="dotFormDialogSave"]'));

            expect(saveButton.attributes.class).toBe('p-button p-component');
            expect(saveButton.attributes['ng-reflect-label']).toBe('Save');
            expect(saveButton.attributes.pButton).toBeDefined();
        });

        it('should have emit save event', () => {
            const saveButton = de.query(By.css('[data-testId="dotFormDialogSave"]'));

            const event = new MouseEvent('click');
            saveButton.triggerEventHandler('click', event);
            expect(component.save.emit).toHaveBeenCalledWith(event);
        });

        it('should have cancel button', () => {
            const cancelButton = de.query(By.css('[data-testId="dotFormDialogCancel"]'));

            expect(cancelButton.attributes.class).toBe('p-button-secondary p-button p-component');
            expect(cancelButton.attributes['ng-reflect-label']).toBe('Cancel');
            expect(cancelButton.attributes.pButton).toBeDefined();
        });

        it('should have emit cancel event', () => {
            const cancelButton = de.query(By.css('[data-testId="dotFormDialogCancel"]'));

            const event = new MouseEvent('click');
            cancelButton.triggerEventHandler('click', event);
            expect(component.cancel.emit).toHaveBeenCalledWith(event);
            expect(dynamicDialogRef.close).toHaveBeenCalledTimes(1);
        });
    });
});

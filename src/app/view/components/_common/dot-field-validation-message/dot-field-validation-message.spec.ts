import { Pipe, PipeTransform } from '@angular/core';

import { TestBed, waitForAsync, ComponentFixture } from '@angular/core/testing';
import { DotFieldValidationMessageComponent } from './dot-field-validation-message';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

@Pipe({
    name: 'dm'
})
class DotMessageMockPipe implements PipeTransform {
    transform(): string {
        return 'Required';
    }
}

describe('FieldValidationComponent', () => {
    let de: DebugElement;
    let el: HTMLElement;
    let fixture: ComponentFixture<DotFieldValidationMessageComponent>;
    let component: DotFieldValidationMessageComponent;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotFieldValidationMessageComponent, DotMessageMockPipe]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFieldValidationMessageComponent);
        component = fixture.debugElement.componentInstance;
    });

    it('should hide the message by default', () => {
        const fakeInput: any = {};
        component.field = fakeInput;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('small'));
        expect(de).toBeNull();
    });

    it('should hide the message when field it is valid', () => {
        const fakeInput: any = {};
        fakeInput.valid = true;
        component.field = fakeInput;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('small'));
        expect(de).toBeNull();
    });

    it('should show the default message when field it is dirty and invalid', () => {
        component.field = {
            dirty: true,
            valid: false,
            enabled: true
        } as any;
        fixture.detectChanges();

        de = fixture.debugElement.query(By.css('small'));
        el = de.nativeElement;
        expect(el).toBeDefined();
        expect(el.textContent).toContain('Required');
    });

    it('should show the message when field it is dirty and invalid', () => {
        component.field = {
            dirty: true,
            valid: false,
            enabled: true
        } as any;
        component.message = 'Error message';
        fixture.detectChanges();

        de = fixture.debugElement.query(By.css('small'));
        el = de.nativeElement;
        expect(el).toBeDefined();
        expect(el.textContent).toContain('Error message');
    });
});

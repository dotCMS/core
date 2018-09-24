import { TestBed, async, ComponentFixture } from '@angular/core/testing';
import { FieldValidationMessageComponent } from './field-validation-message';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

describe('FieldValidationComponent', () => {
    let de: DebugElement;
    let el: HTMLElement;
    let fixture: ComponentFixture<FieldValidationMessageComponent>;
    let component: FieldValidationMessageComponent;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [FieldValidationMessageComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FieldValidationMessageComponent);
        component = fixture.debugElement.componentInstance;
    });

    it('should hide the message by default', () => {
        const fakeInput: any = {};
        component.field = fakeInput;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('div'));
        expect(de).toBeNull();
    });

    it("should hide the message when field it's valid", () => {
        const fakeInput: any = {};
        fakeInput.valid = true;
        component.field = fakeInput;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('div'));
        expect(de).toBeNull();
    });

    it("should show the message when field it's touched and invalid", () => {
        const fakeInput: any = {};
        fakeInput.touched = true;
        fakeInput.valid = false;
        component.field = fakeInput;
        component.message = 'Error message';
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('div'));
        el = de.nativeElement;
        expect(el).toBeDefined();
        expect(el.textContent).toBe('Error message');
    });
});

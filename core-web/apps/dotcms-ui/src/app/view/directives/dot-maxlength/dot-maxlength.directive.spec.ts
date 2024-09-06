import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMaxlengthDirective } from './dot-maxlength.directive';

@Component({
    template: `
        <div contenteditable="true" dotMaxlength="10"></div>
    `
})
class TestComponent {}

function dispatchEvent(element: DebugElement, type: string, textValue: string): void {
    const event = new Event(type);
    element.nativeElement.dispatchEvent(event);
    element.nativeElement.textContent = textValue;
}

describe('DotMaxlengthDirective', () => {
    let fixture: ComponentFixture<TestComponent>;
    let element: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotMaxlengthDirective, TestComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TestComponent);
        element = fixture.debugElement.query(By.css('div'));
        fixture.detectChanges();
    });

    it('should keep same text after event if max length is not reached', fakeAsync(() => {
        dispatchEvent(element, 'keypress', 'test');
        tick(2);
        expect(element.nativeElement.textContent).toBe('test');
    }));

    it('should remove extra characters when length is more than max length on keypress', fakeAsync(() => {
        dispatchEvent(element, 'keypress', '1234567890remove');
        tick(2);
        expect(element.nativeElement.textContent).toBe('1234567890');
    }));

    it('should remove extra characters when length is more than max length on paste', fakeAsync(() => {
        dispatchEvent(element, 'paste', '1234567890remove');
        tick(2);
        expect(element.nativeElement.textContent).toBe('1234567890');
    }));

    it('should prevent default when max length is reached', fakeAsync(() => {
        const event = new Event('keypress');
        spyOn(event, 'preventDefault');
        element.nativeElement.textContent = '12345678901';
        element.nativeElement.dispatchEvent(event);
        tick(2);
        expect(event.preventDefault).toHaveBeenCalled();
    }));
});

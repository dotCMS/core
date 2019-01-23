import { DotMaxlengthDirective } from './dot-maxlength.directive';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

@Component({
    template: `<div contenteditable="true" dotMaxlength="10">test</div>`
})
class TestComponent {}

describe('DotMaxlengthDirective', () => {
    let fixture: ComponentFixture<TestComponent>;
    let element: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotMaxlengthDirective, TestComponent]
        });

        fixture = TestBed.createComponent(TestComponent);
        element = fixture.debugElement.query(By.css('div'));
    });

    it(
        'should allow new characters when max length is not reached',
        fakeAsync(() => {
            const event = new Event('keypress');
            fixture.detectChanges();
            element.nativeElement.dispatchEvent(event);
            element.nativeElement.textContent = element.nativeElement.textContent + 'o';
            tick(5);
            expect(element.nativeElement.textContent).toBe('testo');
        })
    );

    it(
        'should remove extra characters when length is more than max length',
        fakeAsync(() => {
            const event = new Event('paste');
            fixture.detectChanges();
            element.nativeElement.dispatchEvent(event);
            element.nativeElement.textContent = '12345678901';
            tick(5);
            expect(element.nativeElement.textContent).toBe('1234567890');
        })
    );

    it(
        'should prevent default when max length is reached',
        fakeAsync(() => {
            const event = new Event('keypress');
            spyOn(event, 'preventDefault');
            element.nativeElement.textContent = '12345678901';
            fixture.detectChanges();
            element.nativeElement.dispatchEvent(event);
            tick(5);
            expect(event.preventDefault).toHaveBeenCalled();
        })
    );
});

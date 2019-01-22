import { DotMaxlengthDirective } from './dot-maxlength.directive';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

@Component({
    template: `<div contenteditable="true" dotMaxlength="10">123456789</div>`
})
class TestComponent {}

describe('DotMaxlengthDirective', () => {
    let fixture: ComponentFixture<TestComponent>;
    let element: DebugElement;
    let directiveInstance: DotMaxlengthDirective;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotMaxlengthDirective, TestComponent]
        });

        fixture = TestBed.createComponent(TestComponent);
        element = fixture.debugElement.query(By.css('div'));
        directiveInstance = element.injector.get(DotMaxlengthDirective);
    });

    it('should trigger event handler on paste', () => {
        spyOn(directiveInstance, 'eventHandler');
        element.triggerEventHandler('paste', 'Longer text example');
        expect(directiveInstance.eventHandler).toHaveBeenCalled();
    });

    it('should trigger event handler on keypress', () => {
        spyOn(directiveInstance, 'eventHandler');
        element.triggerEventHandler('keypress', { key: 'x' });
        expect(directiveInstance.eventHandler).toHaveBeenCalled();
    });

    it(
        'should remove extra characters',
        fakeAsync(() => {
            fixture.detectChanges();
            element.triggerEventHandler('keypress', { key: 'x' });
            element.nativeElement.textContent = '12345678901';
            tick(5);
            expect(element.nativeElement.textContent).toBe('1234567890');
        })
    );
});

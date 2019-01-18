import { DotMaxlengthDirective } from './dot-maxlength.directive';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

@Component({
    template: `<div contenteditable="true" dotMaxlength="10">12345678901</div>`
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
        element.triggerEventHandler('keypress', 'Longer text example');
        expect(directiveInstance.eventHandler).toHaveBeenCalled();
    });

    it(
        'should remove extra characters',
        fakeAsync(() => {
            fixture.detectChanges();
            directiveInstance.eventHandler();
            tick(5);
            expect(element.nativeElement.textContent).toBe('1234567890');
        })
    );
});

/* tslint:disable:no-unused-variable */

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotAutofocusDirective } from './dot-autofocus.directive';

@Component({
    template: `
        @if (disabled) {
            <input type="text" dotAutofocus disabled />
        } @else {
            <input type="text" dotAutofocus />
        }
    `,
    standalone: false
})
class TestHostComponent {
    disabled = false;

    setDisabled(val: boolean) {
        this.disabled = val;
    }
}

describe('Directive: DotAutofocus', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let component: TestHostComponent;
    let inputEl: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [DotAutofocusDirective]
        }).compileComponents();
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
    });

    it('should call focus', fakeAsync(() => {
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        jest.spyOn(inputEl.nativeElement, 'focus');

        tick(100); // directive uses setTimeout(..., 100) before calling focus

        expect(inputEl.nativeElement.focus).toHaveBeenCalledTimes(1);
    }));

    it('should NOT call focus', fakeAsync(() => {
        component.setDisabled(true);
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        jest.spyOn(inputEl.nativeElement, 'focus');

        tick(100); // advance past directive's setTimeout; focus should not run when disabled
        expect(inputEl.nativeElement.focus).not.toHaveBeenCalled();
    }));
});

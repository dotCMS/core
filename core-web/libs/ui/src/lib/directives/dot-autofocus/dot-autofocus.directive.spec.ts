/* tslint:disable:no-unused-variable */

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotAutofocusDirective } from '@dotcms/ui';

@Component({
    template: `
        @if (disabled) {
            <input type="text" dotAutofocus disabled />
        } @else {
            <input type="text" dotAutofocus />
        }
    `
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

    it('should call focus', async () => {
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        spyOn(inputEl.nativeElement, 'focus');

        await fixture.whenStable();

        expect(inputEl.nativeElement.focus).toHaveBeenCalledTimes(1);
    });

    it('should NOT call focus', async () => {
        component.setDisabled(true);
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        spyOn(inputEl.nativeElement, 'focus');

        await fixture.whenStable();
        expect(inputEl.nativeElement.focus).not.toHaveBeenCalled();
    });
});

/* tslint:disable:no-unused-variable */

import { TestBed, ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';

import { DotAutofocusDirective } from './dot-autofocus.directive';

@Component({
    template: `<input type="text" dotAutofocus />`
})
class TestHostComponent {}

describe('Directive: DotAutofocus', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let inputEl: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotAutofocusDirective]
        });
        fixture = TestBed.createComponent(TestHostComponent);
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        spyOn(inputEl.nativeElement, 'focus');
    });

    it('should call focus', async(() => {
        fixture.whenStable().then(() => {
            expect(inputEl.nativeElement.focus).toHaveBeenCalledTimes(1);
        });
    }));
});

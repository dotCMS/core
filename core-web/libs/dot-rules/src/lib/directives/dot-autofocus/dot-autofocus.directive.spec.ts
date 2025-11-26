import { DebugElement, Component } from '@angular/core';
import { TestBed, ComponentFixture, waitForAsync } from '@angular/core/testing';
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
    imports: [DotAutofocusDirective]
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
            imports: [TestHostComponent]
        });
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
    });

    it('should call focus', waitForAsync(() => {
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        spyOn(inputEl.nativeElement, 'focus');

        fixture.whenStable().then(() => {
            expect(inputEl.nativeElement.focus).toHaveBeenCalledTimes(1);
        });
    }));

    it('should NOT call focus', waitForAsync(() => {
        component.setDisabled(true);
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        spyOn(inputEl.nativeElement, 'focus');

        fixture.whenStable().then(() => {
            expect(inputEl.nativeElement.focus).not.toHaveBeenCalled();
        });
    }));
});

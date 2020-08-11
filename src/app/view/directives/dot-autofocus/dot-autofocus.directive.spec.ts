/* tslint:disable:no-unused-variable */

import { TestBed, ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';

import { DotAutofocusDirective } from './dot-autofocus.directive';

@Component({
    template: `
        <input type="text" dotAutofocus disabled *ngIf="disabled;else not" />
        <ng-template #not>
            <input type="text" dotAutofocus />
        </ng-template>
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
            declarations: [TestHostComponent, DotAutofocusDirective]
        }).compileComponents();
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
    });

    it('should call focus', async(() => {
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        spyOn(inputEl.nativeElement, 'focus');

        fixture.whenStable().then(() => {
            expect(inputEl.nativeElement.focus).toHaveBeenCalledTimes(1);
        });
    }));

    it('should NOT call focus', async(() => {
        component.setDisabled(true);
        fixture.detectChanges();
        inputEl = fixture.debugElement.query(By.css('input'));
        spyOn(inputEl.nativeElement, 'focus');

        fixture.whenStable().then(() => {
            expect(inputEl.nativeElement.focus).not.toHaveBeenCalled();
        });
    }));
});

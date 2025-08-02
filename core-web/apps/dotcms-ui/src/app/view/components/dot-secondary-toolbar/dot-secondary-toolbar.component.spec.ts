import { CommonModule } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotSecondaryToolbarComponent } from './dot-secondary-toolbar.component';

@Component({
    selector: 'dot-test-component',
    template: `
        <dot-secondary-toolbar>
            <div class="main-toolbar-left">1</div>
            <div class="main-toolbar-right">2</div>
            <div class="lower-toolbar-left">3</div>
            <div class="lower-toolbar-right">4</div>
        </dot-secondary-toolbar>
    `,
    standalone: false
})
class HostTestComponent {}

describe('DotSecondaryToolbarComponent', () => {
    let fixture: ComponentFixture<HostTestComponent>;
    let dotToolbarComponent: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [HostTestComponent, DotSecondaryToolbarComponent],
            imports: [CommonModule]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(HostTestComponent);
        fixture.detectChanges();
    });

    it('should have a p-avatar', () => {
        dotToolbarComponent = fixture.debugElement.query(By.css('dot-secondary-toolbar'));
        const primaryToolbarLeft = fixture.debugElement.query(
            By.css('dot-secondary-toolbar .dot-secondary-toolbar__main .main-toolbar-left')
        );
        const primaryToolbarRight = fixture.debugElement.query(
            By.css('dot-secondary-toolbar .dot-secondary-toolbar__main .main-toolbar-right')
        );
        const secondaryToolbarLeft = fixture.debugElement.query(
            By.css('dot-secondary-toolbar .dot-secondary-toolbar__lower .lower-toolbar-left')
        );
        const secondaryToolbarRight = fixture.debugElement.query(
            By.css('dot-secondary-toolbar .dot-secondary-toolbar__lower .lower-toolbar-right')
        );

        expect(dotToolbarComponent).not.toBeNull();
        expect(primaryToolbarLeft.nativeElement.innerText).toBe('1');
        expect(primaryToolbarRight.nativeElement.innerText).toBe('2');
        expect(secondaryToolbarLeft.nativeElement.innerText).toBe('3');
        expect(secondaryToolbarRight.nativeElement.innerText).toBe('4');
    });
});

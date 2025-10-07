import { CommonModule } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { DotSecondaryToolbarComponent } from './dot-secondary-toolbar.component';

import { DotExperimentClassDirective } from '../../../portlets/shared/directives/dot-experiment-class.directive';

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

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [HostTestComponent, DotSecondaryToolbarComponent],
            imports: [CommonModule, RouterTestingModule, DotExperimentClassDirective]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(HostTestComponent);
        fixture.detectChanges();
    });

    it('should have a p-avatar', () => {
        dotToolbarComponent = fixture.debugElement.query(By.css('dot-secondary-toolbar'));

        // Wait for ng-content projection to complete
        fixture.detectChanges();

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

        // Add null checks before accessing innerText
        expect(primaryToolbarLeft).not.toBeNull();
        expect(primaryToolbarRight).not.toBeNull();
        expect(secondaryToolbarLeft).not.toBeNull();
        expect(secondaryToolbarRight).not.toBeNull();

        // Use textContent instead of innerText for Jest/JSDOM compatibility
        expect(primaryToolbarLeft.nativeElement.textContent).toBe('1');
        expect(primaryToolbarRight.nativeElement.textContent).toBe('2');
        expect(secondaryToolbarLeft.nativeElement.textContent).toBe('3');
        expect(secondaryToolbarRight.nativeElement.textContent).toBe('4');
    });
});

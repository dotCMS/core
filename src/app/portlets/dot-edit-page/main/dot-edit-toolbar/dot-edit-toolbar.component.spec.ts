import { ComponentFixture, async } from '@angular/core/testing';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotEditToolbarComponent } from './dot-edit-toolbar.component';

@Component({
    selector: 'dot-test-component',
    template: `
        <dot-edit-toolbar>
            <div class="main-toolbar-left">1</div>
            <div class="main-toolbar-right">2</div>
            <div class="secondary-toolbar-left">3</div>
            <div class="secondary-toolbar-right">4</div>
        </dot-edit-toolbar>
    `
})
class HostTestComponent {}

describe('DotEditToolbarComponent', () => {
    let fixture: ComponentFixture<HostTestComponent>;
    let dotToolbarComponent: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [HostTestComponent, DotEditToolbarComponent],
            imports: [CommonModule]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(HostTestComponent);
        fixture.detectChanges();
    });

    it('should have a dot-avatar', () => {
        dotToolbarComponent = fixture.debugElement.query(By.css('dot-edit-toolbar'));
        const primaryToolbarLeft = fixture.debugElement.query(
            By.css('dot-edit-toolbar .dot-edit-toolbar__main .main-toolbar-left')
        );
        const primaryToolbarRight = fixture.debugElement.query(
            By.css('dot-edit-toolbar .dot-edit-toolbar__main .main-toolbar-right')
        );
        const secondaryToolbarLeft = fixture.debugElement.query(
            By.css('dot-edit-toolbar .dot-edit-toolbar__secondary .secondary-toolbar-left')
        );
        const secondaryToolbarRight = fixture.debugElement.query(
            By.css('dot-edit-toolbar .dot-edit-toolbar__secondary .secondary-toolbar-right')
        );

        expect(dotToolbarComponent).not.toBeNull();
        expect(primaryToolbarLeft.nativeElement.innerText).toBe('1');
        expect(primaryToolbarRight.nativeElement.innerText).toBe('2');
        expect(secondaryToolbarLeft.nativeElement.innerText).toBe('3');
        expect(secondaryToolbarRight.nativeElement.innerText).toBe('4');
    });
});

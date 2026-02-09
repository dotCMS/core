import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';

import { DotFieldHelperComponent } from './dot-field-helper.component';

describe('DotFieldHelperComponent', () => {
    let component: DotFieldHelperComponent;
    let fixture: ComponentFixture<DotFieldHelperComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                DotFieldHelperComponent,
                BrowserAnimationsModule,
                ButtonModule,
                OverlayPanelModule
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotFieldHelperComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.message = 'Hello World';
        fixture.detectChanges();
    });

    it('should display the overlay panel on click', () => {
        const iconButton = de.query(By.css('p-button')).nativeElement;

        iconButton.dispatchEvent(new MouseEvent('click'));
    });

    it('should hide the overlay panel on click', () => {
        const iconButton = de.query(By.css('p-button')).nativeElement;

        iconButton.dispatchEvent(new MouseEvent('click'));
        iconButton.dispatchEvent(new MouseEvent('click'));
    });

    it('should have correct attributes  on button', () => {
        const iconButton = de.query(By.css('p-button')).componentInstance;

        expect(iconButton.icon).toEqual('pi pi-question-circle');
    });

    it('should have correct attributes on Overlay Panel', () => {
        const overlayPanel: OverlayPanel = de.query(By.directive(OverlayPanel)).componentInstance;

        expect(overlayPanel.style).toEqual({ width: '350px' });
        expect(overlayPanel.appendTo).toEqual('body');
        expect(overlayPanel.dismissable).toEqual(true);
    });
});

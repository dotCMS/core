import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotFieldHelperComponent } from './dot-field-helper.component';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { OverlayPanel, OverlayPanelModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconButtonComponent } from '@components/_common/dot-icon-button/dot-icon-button.component';

describe('DotFieldHelperComponent', () => {
    let component: DotFieldHelperComponent;
    let fixture: ComponentFixture<DotFieldHelperComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotFieldHelperComponent],
            imports: [BrowserAnimationsModule, DotIconButtonModule, OverlayPanelModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotFieldHelperComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.message = 'Hello World';
        fixture.detectChanges();
    });

    it('should display the overlay panel on click', () => {
        const iconButton = de.query(By.css('dot-icon-button')).nativeElement;
        const overlayPanel: OverlayPanel = de.query(By.css('p-overlayPanel')).componentInstance;

        iconButton.dispatchEvent(new MouseEvent('click'));

        expect(overlayPanel.visible).toEqual(true);

    });

    it('should hide the overlay panel on click', () => {
        const iconButton = de.query(By.css('dot-icon-button')).nativeElement;
        const overlayPanel: OverlayPanel = de.query(By.css('p-overlayPanel')).componentInstance;

        iconButton.dispatchEvent(new MouseEvent('click'));
        iconButton.dispatchEvent(new MouseEvent('click'));

        expect(overlayPanel.visible).toEqual(false);
    });

    it('should have correct attributes  on button', () => {
        const iconButton: DotIconButtonComponent = de.query(By.css('dot-icon-button'))
            .componentInstance;

        expect(iconButton.icon).toEqual('help_outline');
    });

    it('should have correct attributes on Overlay Panel', () => {
        const overlayPanel: OverlayPanel = de.query(By.css('p-overlayPanel')).componentInstance;

        expect(overlayPanel.style).toEqual({ width: '350px' });
        expect(overlayPanel.appendTo).toEqual('body');
        expect(overlayPanel.dismissable).toEqual(true);
    });
});

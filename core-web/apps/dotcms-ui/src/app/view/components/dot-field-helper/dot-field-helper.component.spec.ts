import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Popover } from 'primeng/popover';

import { DotFieldHelperComponent } from './dot-field-helper.component';

describe('DotFieldHelperComponent', () => {
    let spectator: Spectator<DotFieldHelperComponent>;

    const createComponent = createComponentFactory({
        component: DotFieldHelperComponent,
        imports: [BrowserAnimationsModule]
    });

    beforeEach(() => {
        spectator = createComponent({ props: { message: 'Hello World' } });
    });

    it('should display the overlay panel on click', () => {
        const iconButton = spectator.query('p-button') as HTMLElement;
        expect(iconButton).toBeTruthy();
        iconButton.dispatchEvent(new MouseEvent('click'));
        spectator.detectChanges();
    });

    it('should hide the overlay panel on click', () => {
        const iconButton = spectator.query('p-button') as HTMLElement;
        expect(iconButton).toBeTruthy();
        iconButton.dispatchEvent(new MouseEvent('click'));
        iconButton.dispatchEvent(new MouseEvent('click'));
        spectator.detectChanges();
    });

    it('should have correct attributes on button', () => {
        const iconButtonDe = spectator.debugElement.query(By.css('p-button'));
        expect(iconButtonDe?.componentInstance?.icon).toEqual('pi pi-question-circle');
    });

    it('should have correct attributes on Overlay Panel', () => {
        const overlayPanel: Popover = spectator.debugElement.query(
            By.directive(Popover)
        )?.componentInstance;

        expect(overlayPanel).toBeTruthy();
        expect(overlayPanel.style).toEqual({ width: '350px' });
        const appendTo =
            typeof overlayPanel.appendTo === 'function'
                ? (overlayPanel.appendTo as () => string)()
                : overlayPanel.appendTo;
        expect(appendTo).toEqual('body');
        expect(overlayPanel.dismissable).toEqual(true);
    });
});

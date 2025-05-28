import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotDropdownComponent } from './dot-dropdown.component';

describe('DotDropdownComponent', () => {
    let spectator: Spectator<DotDropdownComponent>;
    const createComponent = createComponentFactory(DotDropdownComponent);

    beforeEach(() =>(spectator = createComponent({
        detectChanges: false
    })));

    describe('Enabled', () => {
        it('should display icon button and emit toggle event when clicked', () => {
            spectator.setInput('icon', 'test');
            spectator.setInput('disabled', false);
            spectator.detectChanges();

            const iconBtn = spectator.query<HTMLButtonElement>(byTestId('icon-button'));
            const spyToggle = spyOn(spectator.component.toggle, 'emit');
            const spyWasOpen = spyOn(spectator.component.wasOpen, 'emit');

            spectator.click(iconBtn);

            const content = spectator.query('.dropdown-content');

            expect(content).toBeTruthy();
            expect(iconBtn).toBeTruthy();
            expect(iconBtn.disabled).toBeFalsy();
            expect(spyToggle).toHaveBeenCalledWith(true);
            expect(spyWasOpen).toHaveBeenCalled();
        });

        it('should display title button and emit toggle event when clicked', () => {
            spectator.setInput('title', 'test');
            spectator.setInput('disabled', false);
            spectator.detectChanges();

            const titleBtn = spectator.query<HTMLButtonElement>(byTestId('title-button'));
            const spyToggle = spyOn(spectator.component.toggle, 'emit');
            const spyWasOpen = spyOn(spectator.component.wasOpen, 'emit');

            spectator.click(titleBtn);

            const content = spectator.query('.dropdown-content');

            expect(content).toBeTruthy();
            expect(titleBtn).toBeTruthy();
            expect(titleBtn.disabled).toBeFalsy();
            expect(spyToggle).toHaveBeenCalledWith(true);
            expect(spyWasOpen).toHaveBeenCalled();
        });
    });

    describe('Disabled', () => {
        it('should disable icon button and not emit when disabled', () => {
            spectator.setInput('icon', 'test');
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const iconBtn = spectator.query<HTMLButtonElement>(byTestId('icon-button'));
            const spyToggle = spyOn(spectator.component.toggle, 'emit');

            expect(iconBtn.disabled).toBeTruthy();

            spectator.click(iconBtn);
            expect(spyToggle).not.toHaveBeenCalled();
        });

        it('should disable title button and not emit when disabled', () => {
            spectator.setInput('title', 'test');
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const titleBtn = spectator.query<HTMLButtonElement>(byTestId('title-button'));
            const spyToggle = spyOn(spectator.component.toggle, 'emit');

            expect(titleBtn.disabled).toBeTruthy();

            spectator.click(titleBtn);
            expect(spyToggle).not.toHaveBeenCalled();
        });
    });

    describe('Dropdown behavior', () => {
        it('should show and hide the dropdown dialog', () => {
            spectator.detectChanges();

            expect(spectator.query('.dropdown-content')).toBeFalsy();

            spectator.component.onToggle();
            spectator.detectChanges();

            expect(spectator.query('.dropdown-content')).toBeTruthy();

            spectator.component.closeIt();
            spectator.detectChanges();

            expect(spectator.query('.dropdown-content')).toBeFalsy();
        });

        it('should emit shutdown event when closing dropdown', () => {
            const spyShutdown = spyOn(spectator.component.shutdown, 'emit');

            spectator.component.onToggle(); // Open
            spectator.component.onToggle(); // Close

            expect(spyShutdown).toHaveBeenCalled();
        });

        it('should show the mask when dropdown is open', () => {
            spectator.detectChanges();
            spectator.component.onToggle();
            spectator.detectChanges();

            const mask = spectator.query('.dot-mask');

            expect(mask).toBeTruthy();
        });

        it('should hide the mask when dropdown is closed', () => {
            spectator.detectChanges();
            spectator.component.onToggle();
            spectator.detectChanges();

            expect(spectator.query('.dot-mask')).toBeTruthy();

            spectator.component.closeIt();
            spectator.detectChanges();

            expect(spectator.query('.dot-mask')).toBeFalsy();
        });

        it('should close dropdown when clicking on mask', () => {
            spectator.detectChanges();
            spectator.component.onToggle();
            spectator.detectChanges();

            const mask = spectator.query('.dot-mask');
            const spyToggle = spyOn(spectator.component.toggle, 'emit');

            spectator.click(mask);

            expect(spyToggle).toHaveBeenCalledWith(false);
        });
    });

    describe('Computed properties', () => {
        it('should compute $disabledIcon correctly when icon is present and disabled', () => {
            spectator.setInput('icon', 'test');
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            expect(spectator.component.$disabledIcon()).toBeTruthy();
        });

        it('should compute $disabledIcon as false when no icon is present', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            expect(spectator.component.$disabledIcon()).toBeFalsy();
        });

        it('should compute $style with correct positioning', () => {
            spectator.setInput('position', 'right');
            spectator.detectChanges();

            expect(spectator.component.$style()).toEqual({ right: '0' });
        });

        it('should compute $style with left positioning by default', () => {
            spectator.detectChanges();

            expect(spectator.component.$style()).toEqual({ left: '0' });
        });
    });

    describe('Click outside behavior', () => {
        it('should close dropdown when clicking outside component', () => {
            spectator.component.onToggle();
            spectator.detectChanges();

            expect(spectator.component.$show()).toBeTruthy();

            // Simulate click outside
            const outsideElement = document.createElement('div');
            document.body.appendChild(outsideElement);

            const clickEvent = new MouseEvent('click', { bubbles: true });
            Object.defineProperty(clickEvent, 'target', { value: outsideElement });

            spectator.component.handleClick(clickEvent);

            expect(spectator.component.$show()).toBeFalsy();

            document.body.removeChild(outsideElement);
        });
    });
});

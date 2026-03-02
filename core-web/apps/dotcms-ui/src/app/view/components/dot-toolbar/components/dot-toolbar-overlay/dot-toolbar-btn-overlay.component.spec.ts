import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { Popover, PopoverModule } from 'primeng/popover';

import { DotToolbarBtnOverlayComponent } from './dot-toolbar-btn-overlay.component';

describe('DotToolbarBtnOverlayComponent', () => {
    let spectator: Spectator<DotToolbarBtnOverlayComponent>;
    let component: DotToolbarBtnOverlayComponent;

    const createComponent = createComponentFactory({
        component: DotToolbarBtnOverlayComponent,
        imports: [PopoverModule, ButtonModule],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('icon', 'pi pi-bell');
        component = spectator.component;
    });

    describe('Component Creation', () => {
        it('should create component successfully', () => {
            expect(component).toBeTruthy();
        });

        it('should initialize with default values', () => {
            expect(component.$showBadge()).toBe(false);
            expect(component.$overlayStyleClass()).toBe('');
            expect(component.$showMask()).toBe(false);
        });

        it('should require icon input', () => {
            expect(component.$icon()).toBe('pi pi-bell');
        });
    });

    describe('Input Signals', () => {
        it('should handle showBadge input signal', () => {
            spectator.setInput('showBadge', true);
            expect(component.$showBadge()).toBe(true);

            spectator.setInput('showBadge', false);
            expect(component.$showBadge()).toBe(false);
        });

        it('should handle overlayStyleClass input signal', () => {
            const customClass = 'custom-overlay-class';
            spectator.setInput('overlayStyleClass', customClass);
            expect(component.$overlayStyleClass()).toBe(customClass);
        });

        it('should handle icon input signal', () => {
            const newIcon = 'pi pi-user';
            spectator.setInput('icon', newIcon);
            expect(component.$icon()).toBe(newIcon);
        });

        it('should handle empty string inputs gracefully', () => {
            spectator.setInput('overlayStyleClass', '');
            spectator.setInput('icon', '');

            expect(component.$overlayStyleClass()).toBe('');
            expect(component.$icon()).toBe('');
        });
    });

    describe('Template Rendering', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should render overlay button with correct attributes', () => {
            const button = spectator.query(byTestId('btn-overlay'));

            expect(button).toBeTruthy();
            expect(button).toHaveClass('overlay-btn');

            // Verify the icon is set by checking the component's input signal
            expect(component.$icon()).toBe('pi pi-bell');
        });

        it('should not show mask initially', () => {
            const mask = spectator.query('.dot-mask');
            expect(mask).not.toExist();
        });

        it('should show mask when $showMask is true', () => {
            component.$showMask.set(true);
            spectator.detectChanges();

            const mask = spectator.query('.dot-mask');
            expect(mask).toExist();
        });

        it('should not show badge initially', () => {
            const badge = spectator.query(byTestId('overlay-badge'));
            expect(badge).not.toExist();
        });

        it('should show badge when showBadge is true', () => {
            spectator.setInput('showBadge', true);
            spectator.detectChanges();

            const badge = spectator.query(byTestId('overlay-badge'));
            expect(badge).toExist();
        });

        it('should apply p-highlight class when mask is shown', () => {
            component.$showMask.set(true);
            spectator.detectChanges();

            const button = spectator.query(byTestId('btn-overlay'));
            expect(button).toHaveClass('p-highlight');
        });

        it('should render overlay panel with correct attributes', () => {
            const overlayPanel = spectator.query('p-popover');

            expect(overlayPanel).toBeTruthy();

            // Access PrimeNG Popover component instance to verify appendTo property
            const overlayPanelDebugElement = spectator.debugElement.query(By.css('p-popover'));
            const overlayPanelComponent = overlayPanelDebugElement?.componentInstance as Popover;
            const appendToValue =
                typeof overlayPanelComponent?.appendTo === 'function'
                    ? overlayPanelComponent.appendTo()
                    : overlayPanelComponent?.appendTo;

            expect(appendToValue).toBe('body');
        });

        it('should apply custom style class to overlay panel', () => {
            const customClass = 'my-custom-class';

            // Create a new spectator with the custom class input set before detection
            const spectatorWithClass = createComponent();
            spectatorWithClass.setInput('icon', 'pi pi-bell');
            spectatorWithClass.setInput('overlayStyleClass', customClass);
            spectatorWithClass.detectChanges();

            const overlayPanel = spectatorWithClass.query('p-popover');
            expect(overlayPanel).toBeTruthy();

            // Access PrimeNG Popover component instance to verify styleClass property
            const overlayPanelDebugElement = spectatorWithClass.debugElement.query(
                By.css('p-popover')
            );
            const overlayPanelComponent = overlayPanelDebugElement?.componentInstance as Popover;
            expect(overlayPanelComponent?.styleClass).toBe(customClass);
        });
    });

    describe('User Interactions', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should toggle overlay panel when button is clicked', () => {
            const button = spectator.query(byTestId('btn-overlay'));

            // Spy on the actual overlay panel's toggle method
            const overlayPanel = component.$overlayPanel();
            jest.spyOn(overlayPanel, 'toggle');

            spectator.click(button);

            expect(overlayPanel.toggle).toHaveBeenCalled();
        });

        it('should hide overlay when mask is clicked', () => {
            component.$showMask.set(true);
            spectator.detectChanges();

            const overlayPanel = component.$overlayPanel();
            jest.spyOn(overlayPanel, 'hide');

            const mask = spectator.query('.dot-mask');
            spectator.click(mask);

            expect(overlayPanel.hide).toHaveBeenCalled();
        });

        it('should handle multiple rapid clicks on button', () => {
            const button = spectator.query(byTestId('btn-overlay'));
            const overlayPanel = component.$overlayPanel();
            jest.spyOn(overlayPanel, 'toggle');

            spectator.click(button);
            spectator.click(button);
            spectator.click(button);

            expect(overlayPanel.toggle).toHaveBeenCalledTimes(3);
        });
    });

    describe('Component Methods', () => {
        describe('handlerShow()', () => {
            it('should set showMask to true', () => {
                expect(component.$showMask()).toBe(false);

                component.handlerShow();

                expect(component.$showMask()).toBe(true);
            });
        });

        describe('handlerHide()', () => {
            it('should set showMask to false and emit onHide event', () => {
                jest.spyOn(component.onHide, 'emit');
                component.$showMask.set(true);

                component.handlerHide();

                expect(component.$showMask()).toBe(false);
                expect(component.onHide.emit).toHaveBeenCalled();
            });

            it('should emit onHide event even when mask was already false', () => {
                jest.spyOn(component.onHide, 'emit');
                component.$showMask.set(false);

                component.handlerHide();

                expect(component.$showMask()).toBe(false);
                expect(component.onHide.emit).toHaveBeenCalled();
            });
        });

        describe('hide()', () => {
            it('should call hide on overlay panel', () => {
                spectator.detectChanges();
                const overlayPanel = component.$overlayPanel();
                jest.spyOn(overlayPanel, 'hide');

                component.hide();

                expect(overlayPanel.hide).toHaveBeenCalled();
            });
        });

        describe('show()', () => {
            it('should call show on overlay panel with event', () => {
                spectator.detectChanges();
                const overlayPanel = component.$overlayPanel();
                jest.spyOn(overlayPanel, 'show');
                const mockEvent = new MouseEvent('click');

                component.show(mockEvent);

                expect(overlayPanel.show).toHaveBeenCalledWith(mockEvent);
                expect(overlayPanel.show).toHaveBeenCalledTimes(1);
            });

            it('should handle show method without errors', () => {
                spectator.detectChanges();
                const mockEvent = new MouseEvent('click');

                expect(() => component.show(mockEvent)).not.toThrow();
            });
        });
    });

    describe('Overlay Panel Events', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should call handlerShow when overlay panel shows', () => {
            jest.spyOn(component, 'handlerShow');

            spectator.triggerEventHandler(Popover, 'onShow', {});

            expect(component.handlerShow).toHaveBeenCalled();
        });

        it('should call handlerHide when overlay panel hides', () => {
            jest.spyOn(component, 'handlerHide');

            spectator.triggerEventHandler(Popover, 'onHide', {});

            expect(component.handlerHide).toHaveBeenCalled();
        });
    });
});

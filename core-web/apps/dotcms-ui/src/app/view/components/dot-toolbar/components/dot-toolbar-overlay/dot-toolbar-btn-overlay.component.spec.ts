import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator';

import { OverlayPanel } from 'primeng/overlaypanel';

import { DotToolbarBtnOverlayComponent } from './dot-toolbar-btn-overlay.component';

describe('DotToolbarBtnOverlayComponent', () => {
    let spectator: Spectator<DotToolbarBtnOverlayComponent>;
    let component: DotToolbarBtnOverlayComponent;

    const createComponent = createComponentFactory({
        component: DotToolbarBtnOverlayComponent,
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
            expect(button?.getAttribute('ng-reflect-icon')).toBe('pi pi-bell');
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
            const badge = spectator.query('.dot-toolbar__badge');
            expect(badge).not.toExist();
        });

        it('should show badge when showBadge is true', () => {
            spectator.setInput('showBadge', true);
            spectator.detectChanges();

            const badge = spectator.query('.dot-toolbar__badge');
            expect(badge).toExist();
        });

        it('should apply isActive class when mask is shown', () => {
            component.$showMask.set(true);
            spectator.detectChanges();

            const button = spectator.query(byTestId('btn-overlay'));
            expect(button).toHaveClass('isActive');
        });

        it('should render overlay panel with correct attributes', () => {
            const overlayPanel = spectator.query('p-overlayPanel');

            expect(overlayPanel).toBeTruthy();
            expect(overlayPanel?.getAttribute('ng-reflect-append-to')).toBe('body');
        });

        it('should apply custom style class to overlay panel', () => {
            const customClass = 'my-custom-class';
            spectator.setInput('overlayStyleClass', customClass);
            spectator.detectChanges();

            const overlayPanel = spectator.query('p-overlayPanel');
            expect(overlayPanel?.getAttribute('ng-reflect-style-class')).toBe(customClass);
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
            spyOn(overlayPanel, 'toggle');

            spectator.click(button);

            expect(overlayPanel.toggle).toHaveBeenCalled();
        });

        it('should hide overlay when mask is clicked', () => {
            component.$showMask.set(true);
            spectator.detectChanges();

            const overlayPanel = component.$overlayPanel();
            spyOn(overlayPanel, 'hide');

            const mask = spectator.query('.dot-mask');
            spectator.click(mask);

            expect(overlayPanel.hide).toHaveBeenCalled();
        });

        it('should handle multiple rapid clicks on button', () => {
            const button = spectator.query(byTestId('btn-overlay'));
            const overlayPanel = component.$overlayPanel();
            spyOn(overlayPanel, 'toggle');

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
                spyOn(component.onHide, 'emit');
                component.$showMask.set(true);

                component.handlerHide();

                expect(component.$showMask()).toBe(false);
                expect(component.onHide.emit).toHaveBeenCalled();
            });

            it('should emit onHide event even when mask was already false', () => {
                spyOn(component.onHide, 'emit');
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
                spyOn(overlayPanel, 'hide');

                component.hide();

                expect(overlayPanel.hide).toHaveBeenCalled();
            });
        });

        describe('show()', () => {
            it('should call show on overlay panel with event', () => {
                spectator.detectChanges();
                const overlayPanel = component.$overlayPanel();
                spyOn(overlayPanel, 'show');
                const mockEvent = new MouseEvent('click');

                component.show(mockEvent);

                expect(overlayPanel.show).toHaveBeenCalledWith(mockEvent);
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
            spyOn(component, 'handlerShow');

            spectator.triggerEventHandler(OverlayPanel, 'onShow', {});

            expect(component.handlerShow).toHaveBeenCalled();
        });

        it('should call handlerHide when overlay panel hides', () => {
            spyOn(component, 'handlerHide');

            spectator.triggerEventHandler(OverlayPanel, 'onHide', {});

            expect(component.handlerHide).toHaveBeenCalled();
        });
    });
});

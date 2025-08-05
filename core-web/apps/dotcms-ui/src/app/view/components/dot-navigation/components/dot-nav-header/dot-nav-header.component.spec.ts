import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { BehaviorSubject } from 'rxjs';

import { ButtonModule } from 'primeng/button';

import { DotNavHeaderComponent } from './dot-nav-header.component';

import { DotNavLogoService } from '../../../../../api/services/dot-nav-logo/dot-nav-logo.service';

describe('DotNavHeaderComponent', () => {
    let spectator: Spectator<DotNavHeaderComponent>;
    let component: DotNavHeaderComponent;
    let dotNavLogoService: SpyObject<DotNavLogoService>;

    const createComponent = createComponentFactory({
        component: DotNavHeaderComponent,
        imports: [ButtonModule],
        providers: [
            mockProvider(DotNavLogoService, {
                navBarLogo$: new BehaviorSubject<string>('')
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        component = spectator.component;
        dotNavLogoService = spectator.inject(DotNavLogoService);
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });

        it('should inject DotNavLogoService', () => {
            expect(dotNavLogoService).toBeTruthy();
        });

        it('should have required inputs and outputs defined', () => {
            expect(component.$isCollapsed).toBeDefined();
            expect(component.toggle).toBeDefined();
            expect(component.$logo).toBeDefined();
        });
    });

    describe('Toggle Button Functionality', () => {
        beforeEach(() => {
            // Set up required input
            spectator.setInput('isCollapsed', false);
            // Provide a default logo value for the service
            dotNavLogoService.navBarLogo$.next(null);
            spectator.detectChanges();
        });

        it('should have pi-bars icon on toggle button', () => {
            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));

            expect(toggleButton).toBeTruthy();
            expect(toggleButton.getAttribute('icon')).toBe('pi pi-bars');
        });

        it('should emit toggle event when button is clicked', () => {
            const spy = spyOn(component.toggle, 'emit');

            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));
            spectator.click(toggleButton);

            expect(spy).toHaveBeenCalledTimes(1);
        });

        it('should emit toggle event with no parameters', () => {
            const spy = spyOn(component.toggle, 'emit');

            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));
            spectator.click(toggleButton);

            expect(spy).toHaveBeenCalledWith();
        });
    });

    describe('Logo Display Logic', () => {
        describe('When navigation is NOT collapsed', () => {
            beforeEach(() => {
                spectator.setInput('isCollapsed', false);
            });

            it('should show logo wrapper when not collapsed', () => {
                dotNavLogoService.navBarLogo$.next(null);
                spectator.detectChanges();

                const logoWrapper = spectator.query('.dot-nav__logo-wrapper');
                expect(logoWrapper).toBeTruthy();
            });

            it('should display default logo when no custom logo is provided', () => {
                dotNavLogoService.navBarLogo$.next(null);
                spectator.detectChanges();

                const defaultLogo = spectator.query(byTestId('default-logo'));
                const whitelabelLogo = spectator.query(byTestId('whitelabel-logo'));

                expect(defaultLogo).toBeTruthy();
                expect(whitelabelLogo).toBeFalsy();
                expect(defaultLogo).toHaveClass('dot-nav__logo--default');
            });

            it('should display whitelabel logo when custom logo is provided', () => {
                const logoUrl = 'url("/dA/logo.png")';
                dotNavLogoService.navBarLogo$.next(logoUrl);
                spectator.detectChanges();

                const whitelabelLogo = spectator.query(byTestId('whitelabel-logo'));
                const defaultLogo = spectator.query(byTestId('default-logo'));

                expect(whitelabelLogo).toBeTruthy();
                expect(defaultLogo).toBeFalsy();
                expect(whitelabelLogo).toHaveClass('dot-nav__logo--whitelabel');
            });

            it('should apply whitelabel class to logo wrapper when custom logo exists', () => {
                const logoUrl = 'url("/dA/logo.png")';
                dotNavLogoService.navBarLogo$.next(logoUrl);
                spectator.detectChanges();

                const logoWrapper = spectator.query('.dot-nav__logo-wrapper');
                expect(logoWrapper).toHaveClass('whitelabel');
            });

            it('should not apply whitelabel class when using default logo', () => {
                dotNavLogoService.navBarLogo$.next(null);
                spectator.detectChanges();

                const logoWrapper = spectator.query('.dot-nav__logo-wrapper');
                expect(logoWrapper).not.toHaveClass('whitelabel');
            });

            it('should set background-image style on whitelabel logo', () => {
                const logoUrl = 'url("/dA/logo.png")';
                dotNavLogoService.navBarLogo$.next(logoUrl);
                spectator.detectChanges();

                const whitelabelLogo = spectator.query(byTestId('whitelabel-logo')) as HTMLElement;
                expect(whitelabelLogo.style.backgroundImage).toBe(logoUrl);
            });
        });

        describe('When navigation IS collapsed', () => {
            beforeEach(() => {
                spectator.setInput('isCollapsed', true);
                dotNavLogoService.navBarLogo$.next(null);
                spectator.detectChanges();
            });

            it('should hide logo wrapper when collapsed', () => {
                const logoWrapper = spectator.query('.dot-nav__logo-wrapper');
                expect(logoWrapper).toBeFalsy();
            });

            it('should not render any logo elements when collapsed', () => {
                const defaultLogo = spectator.query(byTestId('default-logo'));
                const whitelabelLogo = spectator.query(byTestId('whitelabel-logo'));

                expect(defaultLogo).toBeFalsy();
                expect(whitelabelLogo).toBeFalsy();
            });
        });
    });

    describe('Input Signal Behavior', () => {
        it('should react to isCollapsed input changes', () => {
            dotNavLogoService.navBarLogo$.next(null);

            // Initially not collapsed
            spectator.setInput('isCollapsed', false);
            spectator.detectChanges();
            expect(spectator.query('.dot-nav__logo-wrapper')).toBeTruthy();

            // Change to collapsed
            spectator.setInput('isCollapsed', true);
            spectator.detectChanges();
            expect(spectator.query('.dot-nav__logo-wrapper')).toBeFalsy();

            // Change back to not collapsed
            spectator.setInput('isCollapsed', false);
            spectator.detectChanges();
            expect(spectator.query('.dot-nav__logo-wrapper')).toBeTruthy();
        });

        it('should handle isCollapsed signal updates', () => {
            dotNavLogoService.navBarLogo$.next(null);

            spectator.setInput('isCollapsed', false);
            spectator.detectChanges();

            expect(component.$isCollapsed()).toBe(false);

            spectator.setInput('isCollapsed', true);
            spectator.detectChanges();

            expect(component.$isCollapsed()).toBe(true);
        });
    });

    describe('Logo Service Integration', () => {
        beforeEach(() => {
            spectator.setInput('isCollapsed', false);
        });

        it('should sync with logo service observable', () => {
            const logoUrl = 'url("/dA/test-logo.png")';

            dotNavLogoService.navBarLogo$.next(logoUrl);
            spectator.detectChanges();

            expect(component.$logo()).toBe(logoUrl);
        });

        it('should handle multiple logo changes from service', () => {
            // First logo
            const firstLogo = 'url("/dA/logo1.png")';
            dotNavLogoService.navBarLogo$.next(firstLogo);
            spectator.detectChanges();

            expect(component.$logo()).toBe(firstLogo);
            expect(spectator.query(byTestId('whitelabel-logo'))).toBeTruthy();

            // Change to no logo
            dotNavLogoService.navBarLogo$.next(null);
            spectator.detectChanges();

            expect(component.$logo()).toBe(null);
            expect(spectator.query(byTestId('default-logo'))).toBeTruthy();

            // Change to different logo
            const secondLogo = 'url("/dA/logo2.png")';
            dotNavLogoService.navBarLogo$.next(secondLogo);
            spectator.detectChanges();

            expect(component.$logo()).toBe(secondLogo);
            expect(spectator.query(byTestId('whitelabel-logo'))).toBeTruthy();
        });

        it('should handle empty string logo from service', () => {
            dotNavLogoService.navBarLogo$.next('');
            spectator.detectChanges();

            // Empty string should be treated as falsy, show default logo
            expect(spectator.query(byTestId('default-logo'))).toBeTruthy();
            expect(spectator.query(byTestId('whitelabel-logo'))).toBeFalsy();
        });

        it('should handle undefined logo from service', () => {
            dotNavLogoService.navBarLogo$.next(undefined);
            spectator.detectChanges();

            // Undefined should show default logo
            expect(spectator.query(byTestId('default-logo'))).toBeTruthy();
            expect(spectator.query(byTestId('whitelabel-logo'))).toBeFalsy();
        });
    });

    describe('Template Structure and Classes', () => {
        beforeEach(() => {
            spectator.setInput('isCollapsed', false);
            dotNavLogoService.navBarLogo$.next(null);
            spectator.detectChanges();
        });

        it('should have correct CSS classes structure', () => {
            const header = spectator.query('.dot-nav__header');
            const buttonWrapper = spectator.query('.dot-nav__button-wrapper');
            const logoWrapper = spectator.query('.dot-nav__logo-wrapper');

            expect(header).toBeTruthy();
            expect(buttonWrapper).toBeTruthy();
            expect(logoWrapper).toBeTruthy();
        });

        it('should use template variables correctly', () => {
            // The template uses @let variables for isCollapsed and logo
            // We can verify they work by checking the rendered content
            spectator.setInput('isCollapsed', true);
            spectator.detectChanges();

            // Logo wrapper should not be present when collapsed
            expect(spectator.query('.dot-nav__logo-wrapper')).toBeFalsy();

            spectator.setInput('isCollapsed', false);
            spectator.detectChanges();

            // Logo wrapper should be present when not collapsed
            expect(spectator.query('.dot-nav__logo-wrapper')).toBeTruthy();
        });
    });

    describe('Component Output Events', () => {
        it('should emit toggle event multiple times', () => {
            spectator.setInput('isCollapsed', false);
            dotNavLogoService.navBarLogo$.next(null);
            spectator.detectChanges();

            const spy = spyOn(component.toggle, 'emit');
            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));

            // Click multiple times
            spectator.click(toggleButton);
            spectator.click(toggleButton);
            spectator.click(toggleButton);

            expect(spy).toHaveBeenCalledTimes(3);
        });

        it('should emit toggle event even when collapsed', () => {
            spectator.setInput('isCollapsed', true);
            dotNavLogoService.navBarLogo$.next(null);
            spectator.detectChanges();

            const spy = spyOn(component.toggle, 'emit');
            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));

            spectator.click(toggleButton);

            expect(spy).toHaveBeenCalledTimes(1);
        });
    });

    describe('Accessibility and User Experience', () => {
        beforeEach(() => {
            spectator.setInput('isCollapsed', false);
            dotNavLogoService.navBarLogo$.next(null);
            spectator.detectChanges();
        });

        it('should have accessible button element', () => {
            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));
            expect(toggleButton).toBeTruthy();
            expect(toggleButton.tagName.toLowerCase()).toBe('button');
        });

        it('should maintain button functionality across logo changes', () => {
            const spy = spyOn(component.toggle, 'emit');
            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));

            // Test with default logo
            spectator.click(toggleButton);
            expect(spy).toHaveBeenCalledTimes(1);

            // Change to whitelabel logo
            dotNavLogoService.navBarLogo$.next('url("/dA/logo.png")');
            spectator.detectChanges();

            // Button should still work
            spectator.click(toggleButton);
            expect(spy).toHaveBeenCalledTimes(2);
        });

        it('should maintain consistent testid attributes', () => {
            // Test that testid attributes are always present for testing
            const toggleButton = spectator.query(byTestId('dot-nav-header-toggle-button'));
            expect(toggleButton).toBeTruthy();

            const defaultLogo = spectator.query(byTestId('default-logo'));
            expect(defaultLogo).toBeTruthy();

            // Change to whitelabel
            dotNavLogoService.navBarLogo$.next('url("/dA/logo.png")');
            spectator.detectChanges();

            const whitelabelLogo = spectator.query(byTestId('whitelabel-logo'));
            expect(whitelabelLogo).toBeTruthy();
        });
    });

    describe('Component State Consistency', () => {
        it('should maintain consistent state when inputs change', () => {
            // Test state consistency across multiple changes
            const states = [
                { isCollapsed: false, logo: null },
                { isCollapsed: true, logo: null },
                { isCollapsed: false, logo: 'url("/dA/logo1.png")' },
                { isCollapsed: true, logo: 'url("/dA/logo1.png")' },
                { isCollapsed: false, logo: 'url("/dA/logo2.png")' },
                { isCollapsed: false, logo: null }
            ];

            states.forEach((state) => {
                spectator.setInput('isCollapsed', state.isCollapsed);
                dotNavLogoService.navBarLogo$.next(state.logo);
                spectator.detectChanges();

                // Verify component state
                expect(component.$isCollapsed()).toBe(state.isCollapsed);
                expect(component.$logo()).toBe(state.logo);

                // Verify UI state
                const logoWrapper = spectator.query('.dot-nav__logo-wrapper');
                if (state.isCollapsed) {
                    expect(logoWrapper).toBeFalsy();
                } else {
                    expect(logoWrapper).toBeTruthy();

                    if (state.logo) {
                        expect(spectator.query(byTestId('whitelabel-logo'))).toBeTruthy();
                        expect(spectator.query(byTestId('default-logo'))).toBeFalsy();
                    } else {
                        expect(spectator.query(byTestId('default-logo'))).toBeTruthy();
                        expect(spectator.query(byTestId('whitelabel-logo'))).toBeFalsy();
                    }
                }
            });
        });
    });
});

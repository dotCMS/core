import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { DotSidebarAccordionTabComponent } from './dot-sidebar-accordion-tab.component';

describe('DotSidebarAccordionTabComponent', () => {
    let spectator: Spectator<DotSidebarAccordionTabComponent>;

    const createComponent = createComponentFactory({
        component: DotSidebarAccordionTabComponent
    });

    describe('Component Creation and DOM Structure', () => {
        it('should create component without crashing', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-id');
            spectator.setInput('label', 'Test Label');

            expect(spectator.component).toBeTruthy();
        });

        it('should render component host element', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-tab');
            spectator.setInput('label', 'Test Tab');

            // In isolated testing, Spectator creates a wrapper div
            expect(spectator.element).toBeInstanceOf(HTMLElement);
            expect(spectator.element.tagName.toLowerCase()).toBe('div');
        });

        it('should render without visible content in isolation', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-tab');
            spectator.setInput('label', 'Test Tab');

            // In isolation, component has no visible content (content projection works only with parent)
            expect(spectator.element.children.length).toBe(0);
            expect(spectator.element.textContent?.trim()).toBe('');
        });
    });

    describe('Input Properties and Reactivity', () => {
        it('should handle id input correctly', () => {
            spectator = createComponent();
            spectator.setInput('id', 'unique-tab-id');
            spectator.setInput('label', 'Test Label');

            expect(spectator.component.$id()).toBe('unique-tab-id');
        });

        it('should handle label input correctly', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-id');
            spectator.setInput('label', 'My Tab Label');

            expect(spectator.component.$label()).toBe('My Tab Label');
        });

        it('should handle disabled state correctly', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-id');
            spectator.setInput('label', 'Test Label');
            spectator.setInput('disabled', true);

            expect(spectator.component.$disabled()).toBe(true);
        });

        it('should have disabled as false by default', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-id');
            spectator.setInput('label', 'Test Label');

            expect(spectator.component.$disabled()).toBe(false);
        });

        it('should update properties when inputs change', () => {
            spectator = createComponent();
            spectator.setInput('id', 'initial-id');
            spectator.setInput('label', 'Initial Label');
            spectator.setInput('disabled', false);

            expect(spectator.component.$id()).toBe('initial-id');
            expect(spectator.component.$label()).toBe('Initial Label');
            expect(spectator.component.$disabled()).toBe(false);

            // Update inputs
            spectator.setInput('id', 'updated-id');
            spectator.setInput('label', 'Updated Label');
            spectator.setInput('disabled', true);

            expect(spectator.component.$id()).toBe('updated-id');
            expect(spectator.component.$label()).toBe('Updated Label');
            expect(spectator.component.$disabled()).toBe(true);
        });
    });

    describe('Template Reference Management', () => {
        it('should have tabContent template reference available', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-tab');
            spectator.setInput('label', 'Test Tab');

            expect(spectator.component.tabContent).toBeDefined();
            expect(spectator.component.tabContent).toBeTruthy();
        });

        it('should have headerContent template reference available', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-tab');
            spectator.setInput('label', 'Test Tab');

            expect(spectator.component.headerContent).toBeDefined();
            expect(spectator.component.headerContent).toBeTruthy();
        });

        it('should maintain template reference through property changes', () => {
            spectator = createComponent();
            spectator.setInput('id', 'test-tab');
            spectator.setInput('label', 'Test Tab');

            const initialTabTemplate = spectator.component.tabContent;
            const initialHeaderTemplate = spectator.component.headerContent;
            expect(initialTabTemplate).toBeDefined();
            expect(initialHeaderTemplate).toBeDefined();

            // Change properties
            spectator.setInput('label', 'Updated Label');
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            // Template references should remain the same
            expect(spectator.component.tabContent).toBe(initialTabTemplate);
            expect(spectator.component.headerContent).toBe(initialHeaderTemplate);
        });
    });

    describe('Component Integration Readiness', () => {
        it('should be ready for parent accordion integration', () => {
            spectator = createComponent();
            spectator.setInput('id', 'integration-test');
            spectator.setInput('label', 'Integration Tab');
            spectator.setInput('disabled', false);

            // Verify all required properties for parent component integration
            expect(spectator.component.$id()).toBeDefined();
            expect(spectator.component.$label()).toBeDefined();
            expect(spectator.component.$disabled()).toBeDefined();
            expect(spectator.component.tabContent).toBeDefined();
            expect(spectator.component.headerContent).toBeDefined();

            // Properties should have correct values
            expect(spectator.component.$id()).toBe('integration-test');
            expect(spectator.component.$label()).toBe('Integration Tab');
            expect(spectator.component.$disabled()).toBe(false);
        });
    });
});

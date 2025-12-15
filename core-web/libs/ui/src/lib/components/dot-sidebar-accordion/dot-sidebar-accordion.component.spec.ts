import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator/jest';

import { AnimationEvent } from '@angular/animations';
import { Component } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotSidebarAccordionTabComponent } from './components/dot-sidebar-accordion-tab/dot-sidebar-accordion-tab.component';
import { DotSidebarAccordionComponent } from './dot-sidebar-accordion.component';

// Test host component that properly uses the accordion
@Component({
    template: `
        <dot-sidebar-accordion
            [initialActiveTab]="initialActiveTab"
            (activeTabChange)="onActiveTabChange($event)"
            data-testid="accordion">
            <dot-sidebar-accordion-tab id="tab1" label="Tab 1" [disabled]="tab1Disabled">
                <div data-testid="tab1-content">Tab 1 Content</div>
            </dot-sidebar-accordion-tab>

            <dot-sidebar-accordion-tab id="tab2" label="Tab 2">
                <div data-testid="tab2-content">Tab 2 Content</div>
            </dot-sidebar-accordion-tab>

            <dot-sidebar-accordion-tab id="tab3" label="Tab 3" [disabled]="tab3Disabled">
                <div data-testid="tab3-content">Tab 3 Content</div>
            </dot-sidebar-accordion-tab>
        </dot-sidebar-accordion>

        <div data-testid="emitted-value">{{ lastEmittedValue }}</div>
    `,
    imports: [DotSidebarAccordionComponent, DotSidebarAccordionTabComponent]
})
class TestHostComponent {
    initialActiveTab: string | null = null;
    tab1Disabled = false;
    tab3Disabled = false;
    lastEmittedValue: string | null = null;

    onActiveTabChange(value: string | null) {
        this.lastEmittedValue = value;
    }
}

// Test host component with header content
@Component({
    template: `
        <dot-sidebar-accordion
            [initialActiveTab]="initialActiveTab"
            (activeTabChange)="onActiveTabChange($event)"
            data-testid="accordion-with-header-content">
            <dot-sidebar-accordion-tab id="tab1" label="Tab 1">
                <button
                    slot="header-content"
                    data-testid="tab1-header-button"
                    (click)="onHeaderButtonClick('tab1')">
                    Action 1
                </button>
                <div data-testid="tab1-content">Tab 1 Content</div>
            </dot-sidebar-accordion-tab>

            <dot-sidebar-accordion-tab id="tab2" label="Tab 2">
                <div slot="header-content" data-testid="tab2-header-content">
                    <span>Custom Content</span>
                    <button data-testid="tab2-header-button" (click)="onHeaderButtonClick('tab2')">
                        Action 2
                    </button>
                </div>
                <div data-testid="tab2-content">Tab 2 Content</div>
            </dot-sidebar-accordion-tab>

            <dot-sidebar-accordion-tab id="tab3" label="Tab 3">
                <div data-testid="tab3-content">Tab 3 Content (No Header Content)</div>
            </dot-sidebar-accordion-tab>
        </dot-sidebar-accordion>

        <div data-testid="emitted-value">{{ lastEmittedValue }}</div>
        <div data-testid="header-button-clicked">{{ headerButtonClicked }}</div>
    `,
    imports: [DotSidebarAccordionComponent, DotSidebarAccordionTabComponent]
})
class TestHostWithHeaderContentComponent {
    initialActiveTab: string | null = null;
    lastEmittedValue: string | null = null;
    headerButtonClicked: string | null = null;

    onActiveTabChange(value: string | null) {
        this.lastEmittedValue = value;
    }

    onHeaderButtonClick(tabId: string) {
        this.headerButtonClicked = tabId;
    }
}

describe('DotSidebarAccordionComponent', () => {
    let spectator: Spectator<TestHostComponent>;

    const createComponent = createComponentFactory({
        component: TestHostComponent,
        imports: [NoopAnimationsModule]
    });

    describe('DOM Structure and Rendering', () => {
        it('should render accordion with proper structure', () => {
            spectator = createComponent();

            // Main accordion container should exist
            expect(spectator.query('.dot-sidebar-accordion')).toExist();

            // Should render all 3 tabs
            const tabs = spectator.queryAll('.accordion-tab');
            expect(tabs).toHaveLength(3);

            // Each tab should have header and content
            tabs.forEach((tab) => {
                expect(tab.querySelector('.accordion-header')).toExist();
                expect(tab.querySelector('.accordion-header__left')).toExist();
                expect(tab.querySelector('.accordion-header__right')).toExist();
                expect(tab.querySelector('.accordion-content')).toExist();
            });
        });

        it('should render tab labels correctly', () => {
            spectator = createComponent();

            const headers = spectator.queryAll('.accordion-header');
            expect(headers[0].querySelector('.accordion-header__left span')).toHaveText('Tab 1');
            expect(headers[1].querySelector('.accordion-header__left span')).toHaveText('Tab 2');
            expect(headers[2].querySelector('.accordion-header__left span')).toHaveText('Tab 3');
        });

        it('should render chevron icons in all headers', () => {
            spectator = createComponent();

            const headers = spectator.queryAll('.accordion-header');
            headers.forEach((header) => {
                expect(header.querySelector('.accordion-header__left i.pi-chevron-down')).toExist();
            });
        });

        it('should project content correctly', () => {
            spectator = createComponent();

            expect(spectator.query(byTestId('tab1-content'))).toHaveText('Tab 1 Content');
            expect(spectator.query(byTestId('tab2-content'))).toHaveText('Tab 2 Content');
            expect(spectator.query(byTestId('tab3-content'))).toHaveText('Tab 3 Content');
        });
    });

    describe('Initial State', () => {
        it('should render with no active tab by default', () => {
            spectator = createComponent();

            const accordion = spectator.query('.dot-sidebar-accordion');
            expect(accordion).not.toHaveClass('dot-sidebar-accordion--has-active');

            const tabs = spectator.queryAll('.accordion-tab');
            tabs.forEach((tab) => {
                expect(tab).not.toHaveClass('active');
            });

            const contents = spectator.queryAll('.accordion-content');
            contents.forEach((content) => {
                expect(content).toHaveClass('accordion-content--collapsed');
            });
        });

        it('should render with initial active tab when specified', () => {
            spectator = createComponent();
            spectator.component.initialActiveTab = 'tab2';
            spectator.detectChanges();

            // Trigger lifecycle to activate initial tab
            const accordionComponent = spectator.query(DotSidebarAccordionComponent);
            if (accordionComponent) {
                accordionComponent.ngAfterViewInit();
            }
            spectator.detectChanges();

            // DOM should reflect active state
            const accordion = spectator.query('.dot-sidebar-accordion');
            expect(accordion).toHaveClass('dot-sidebar-accordion--has-active');

            const tabs = spectator.queryAll('.accordion-tab');
            expect(tabs[0]).not.toHaveClass('active');
            expect(tabs[1]).toHaveClass('active');
            expect(tabs[2]).not.toHaveClass('active');

            const contents = spectator.queryAll('.accordion-content');
            expect(contents[0]).toHaveClass('accordion-content--collapsed');
            expect(contents[1]).toHaveClass('accordion-content--expanded');
            expect(contents[2]).toHaveClass('accordion-content--collapsed');
        });
    });

    describe('Click Interactions', () => {
        it('should activate tab when clicking header', () => {
            spectator = createComponent();

            // Click on tab2 header
            const tab2Header = spectator.queryAll('.accordion-header')[1];
            spectator.click(tab2Header);
            spectator.detectChanges();

            // DOM should show tab2 as active
            const tabs = spectator.queryAll('.accordion-tab');
            expect(tabs[1]).toHaveClass('active');

            const accordion = spectator.query('.dot-sidebar-accordion');
            expect(accordion).toHaveClass('dot-sidebar-accordion--has-active');
        });

        it('should emit activeTabChange event when tab is clicked', () => {
            spectator = createComponent();

            // Click on tab1 header
            spectator.click(spectator.queryAll('.accordion-header')[0]);
            spectator.detectChanges();

            // Should emit the tab ID
            expect(spectator.query(byTestId('emitted-value'))).toHaveText('tab1');
        });

        it('should switch between tabs correctly', () => {
            spectator = createComponent();

            // Click on tab1
            spectator.click(spectator.queryAll('.accordion-header')[0]);
            spectator.detectChanges();

            const tabs = spectator.queryAll('.accordion-tab');
            expect(tabs[0]).toHaveClass('active');
            expect(spectator.query(byTestId('emitted-value'))).toHaveText('tab1');

            // Click on tab3 - should switch
            spectator.click(spectator.queryAll('.accordion-header')[2]);
            spectator.detectChanges();

            // Simulate animation completion for proper tab switching
            const accordionComponent = spectator.query(DotSidebarAccordionComponent);
            if (accordionComponent) {
                const mockAnimationEvent = {
                    toState: 'collapsed',
                    fromState: 'expanded',
                    totalTime: 0,
                    phaseName: 'done',
                    element: spectator.element,
                    triggerName: 'slideInOut',
                    disabled: false
                } as AnimationEvent;
                accordionComponent.onAnimationDone(mockAnimationEvent, 'tab1');
            }
            spectator.detectChanges();

            expect(tabs[0]).not.toHaveClass('active');
            expect(tabs[2]).toHaveClass('active');
            expect(spectator.query(byTestId('emitted-value'))).toHaveText('tab3');
        });

        it('should keep tab active when clicking same tab again', () => {
            spectator = createComponent();

            // Click on tab2
            spectator.click(spectator.queryAll('.accordion-header')[1]);
            spectator.detectChanges();

            const tabs = spectator.queryAll('.accordion-tab');
            expect(tabs[1]).toHaveClass('active');

            // Click on tab2 again
            spectator.click(spectator.queryAll('.accordion-header')[1]);
            spectator.detectChanges();

            // Should remain active (accordion behavior)
            expect(tabs[1]).toHaveClass('active');
        });
    });

    describe('Disabled State', () => {
        it('should apply disabled classes to disabled tabs', () => {
            spectator = createComponent();
            spectator.component.tab1Disabled = true;
            spectator.component.tab3Disabled = true;
            spectator.detectChanges();

            const tabs = spectator.queryAll('.accordion-tab');
            const headers = spectator.queryAll('.accordion-header');

            // Disabled tabs should have disabled classes
            expect(tabs[0]).toHaveClass('accordion-tab--disabled');
            expect(tabs[1]).not.toHaveClass('accordion-tab--disabled');
            expect(tabs[2]).toHaveClass('accordion-tab--disabled');

            expect(headers[0]).toHaveClass('accordion-header--disabled');
            expect(headers[1]).not.toHaveClass('accordion-header--disabled');
            expect(headers[2]).toHaveClass('accordion-header--disabled');
        });

        it('should not respond to clicks on disabled tabs', () => {
            spectator = createComponent();
            spectator.component.tab1Disabled = true;
            spectator.detectChanges();

            // Click on disabled tab1
            spectator.click(spectator.queryAll('.accordion-header')[0]);
            spectator.detectChanges();

            // Tab should not become active
            const tabs = spectator.queryAll('.accordion-tab');
            expect(tabs[0]).not.toHaveClass('active');

            // No event should be emitted
            expect(spectator.query(byTestId('emitted-value'))).toHaveText('');

            // Accordion should not have active class
            const accordion = spectator.query('.dot-sidebar-accordion');
            expect(accordion).not.toHaveClass('dot-sidebar-accordion--has-active');
        });

        it('should allow clicking enabled tabs when others are disabled', () => {
            spectator = createComponent();
            spectator.component.tab1Disabled = true;
            spectator.component.tab3Disabled = true;
            spectator.detectChanges();

            // Click on enabled tab2
            spectator.click(spectator.queryAll('.accordion-header')[1]);
            spectator.detectChanges();

            // Tab2 should become active
            const tabs = spectator.queryAll('.accordion-tab');
            expect(tabs[1]).toHaveClass('active');
            expect(spectator.query(byTestId('emitted-value'))).toHaveText('tab2');
        });
    });

    describe('CSS Classes and Visual State', () => {
        it('should apply correct base CSS classes', () => {
            spectator = createComponent();

            const accordion = spectator.query('.dot-sidebar-accordion');
            expect(accordion).toBeTruthy();
            expect(accordion).not.toHaveClass('dot-sidebar-accordion--has-active');
            expect(accordion).not.toHaveClass('dot-sidebar-accordion--transitioning');
        });

        it('should toggle accordion active class based on tab state', () => {
            spectator = createComponent();

            const accordion = spectator.query('.dot-sidebar-accordion');
            expect(accordion).not.toHaveClass('dot-sidebar-accordion--has-active');

            // Activate a tab
            spectator.click(spectator.queryAll('.accordion-header')[0]);
            spectator.detectChanges();

            expect(accordion).toHaveClass('dot-sidebar-accordion--has-active');
        });

        it('should apply expanded/collapsed classes correctly', () => {
            spectator = createComponent();

            // Initially all collapsed
            const contents = spectator.queryAll('.accordion-content');
            contents.forEach((content) => {
                expect(content).toHaveClass('accordion-content--collapsed');
                expect(content).not.toHaveClass('accordion-content--expanded');
            });

            // Activate tab2
            spectator.click(spectator.queryAll('.accordion-header')[1]);
            spectator.detectChanges();

            // Tab2 content should be expanded, others collapsed
            expect(contents[0]).toHaveClass('accordion-content--collapsed');
            expect(contents[1]).toHaveClass('accordion-content--expanded');
            expect(contents[2]).toHaveClass('accordion-content--collapsed');
        });
    });

    describe('Event Emission', () => {
        it('should emit null when no tab is active initially', () => {
            spectator = createComponent();

            expect(spectator.query(byTestId('emitted-value'))).toHaveText('');
        });

        it('should emit correct tab ID when tab becomes active', () => {
            spectator = createComponent();

            spectator.click(spectator.queryAll('.accordion-header')[2]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('emitted-value'))).toHaveText('tab3');
        });

        it('should emit events for tab switching', () => {
            spectator = createComponent();

            // Activate tab1
            spectator.click(spectator.queryAll('.accordion-header')[0]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('emitted-value'))).toHaveText('tab1');

            // Switch to tab2
            spectator.click(spectator.queryAll('.accordion-header')[1]);
            spectator.detectChanges();

            // Simulate animation completion
            const accordionComponent = spectator.query(DotSidebarAccordionComponent);
            if (accordionComponent) {
                const mockAnimationEvent = {
                    toState: 'collapsed',
                    fromState: 'expanded',
                    totalTime: 0,
                    phaseName: 'done',
                    element: spectator.element,
                    triggerName: 'slideInOut',
                    disabled: false
                } as AnimationEvent;
                accordionComponent.onAnimationDone(mockAnimationEvent, 'tab1');
            }
            spectator.detectChanges();

            expect(spectator.query(byTestId('emitted-value'))).toHaveText('tab2');
        });
    });

    describe('Edge Cases', () => {
        it('should handle invalid initial active tab gracefully', () => {
            spectator = createComponent();
            spectator.component.initialActiveTab = 'non-existent-tab';
            spectator.detectChanges();

            // Should not crash and no tabs should be active
            const tabs = spectator.queryAll('.accordion-tab');
            tabs.forEach((tab) => {
                expect(tab).not.toHaveClass('active');
            });

            const accordion = spectator.query('.dot-sidebar-accordion');
            expect(accordion).not.toHaveClass('dot-sidebar-accordion--has-active');
        });

        it('should handle rapid clicks gracefully', () => {
            spectator = createComponent();

            const headers = spectator.queryAll('.accordion-header');

            // Rapid clicks should not crash
            expect(() => {
                spectator.click(headers[0]);
                spectator.click(headers[1]);
                spectator.click(headers[2]);
                spectator.click(headers[0]);
                spectator.detectChanges();
            }).not.toThrow();
        });

        it('should maintain DOM structure integrity after interactions', () => {
            spectator = createComponent();

            // Perform various interactions
            spectator.click(spectator.queryAll('.accordion-header')[0]);
            spectator.detectChanges();

            spectator.click(spectator.queryAll('.accordion-header')[1]);
            spectator.detectChanges();

            // DOM structure should remain intact
            expect(spectator.queryAll('.accordion-tab')).toHaveLength(3);
            expect(spectator.queryAll('.accordion-header')).toHaveLength(3);
            expect(spectator.queryAll('.accordion-content')).toHaveLength(3);

            // Content should still be projected
            expect(spectator.query(byTestId('tab1-content'))).toExist();
            expect(spectator.query(byTestId('tab2-content'))).toExist();
            expect(spectator.query(byTestId('tab3-content'))).toExist();
        });
    });

    describe('Header Content Projection', () => {
        let spectatorWithHeaderContent: Spectator<TestHostWithHeaderContentComponent>;

        const createComponentWithHeaderContent = createComponentFactory({
            component: TestHostWithHeaderContentComponent,
            imports: [NoopAnimationsModule]
        });

        it('should render header content in the right section', () => {
            spectatorWithHeaderContent = createComponentWithHeaderContent();

            // Tab1 should have button in header
            const tab1HeaderRight = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(1) .accordion-header__right'
            );
            expect(tab1HeaderRight?.querySelector('[data-testid="tab1-header-button"]')).toExist();
            expect(tab1HeaderRight?.querySelector('button')).toHaveText('Action 1');

            // Tab2 should have custom content in header
            const tab2HeaderRight = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(2) .accordion-header__right'
            );
            expect(tab2HeaderRight?.querySelector('[data-testid="tab2-header-content"]')).toExist();
            expect(tab2HeaderRight?.querySelector('span')).toHaveText('Custom Content');
            expect(tab2HeaderRight?.querySelector('[data-testid="tab2-header-button"]')).toExist();

            // Tab3 should have empty header right section
            const tab3HeaderRight = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(3) .accordion-header__right'
            );
            expect(tab3HeaderRight).toExist();
            expect(tab3HeaderRight?.textContent?.trim()).toBe('');
        });

        it('should not trigger accordion toggle when clicking header content', () => {
            spectatorWithHeaderContent = createComponentWithHeaderContent();

            // Click on header button should not activate tab
            const headerButton = spectatorWithHeaderContent.query(
                '[data-testid="tab1-header-button"]'
            );
            expect(headerButton).toBeTruthy();
            spectatorWithHeaderContent.click(headerButton);
            spectatorWithHeaderContent.detectChanges();

            // Tab should not be active
            const tab1 = spectatorWithHeaderContent.query('.accordion-tab:nth-child(1)');
            expect(tab1).not.toHaveClass('active');

            // But button click should be registered
            expect(spectatorWithHeaderContent.query(byTestId('header-button-clicked'))).toHaveText(
                'tab1'
            );
        });

        it('should allow accordion toggle when clicking left side of header', () => {
            spectatorWithHeaderContent = createComponentWithHeaderContent();

            // Click on left side (label area) should activate tab
            const headerLeft = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(1) .accordion-header__left'
            );
            expect(headerLeft).toBeTruthy();
            spectatorWithHeaderContent.click(headerLeft);
            spectatorWithHeaderContent.detectChanges();

            // Tab should be active
            const tab1 = spectatorWithHeaderContent.query('.accordion-tab:nth-child(1)');
            expect(tab1).toHaveClass('active');

            // Event should be emitted
            expect(spectatorWithHeaderContent.query(byTestId('emitted-value'))).toHaveText('tab1');
        });

        it('should maintain header content when tab becomes active/inactive', () => {
            spectatorWithHeaderContent = createComponentWithHeaderContent();

            // Activate tab1
            const headerLeft = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(1) .accordion-header__left'
            );
            expect(headerLeft).toBeTruthy();
            spectatorWithHeaderContent.click(headerLeft);
            spectatorWithHeaderContent.detectChanges();

            // Header content should still be there
            const tab1HeaderRight = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(1) .accordion-header__right'
            );
            expect(tab1HeaderRight?.querySelector('[data-testid="tab1-header-button"]')).toExist();

            // Switch to tab2
            const tab2HeaderLeft = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(2) .accordion-header__left'
            );
            expect(tab2HeaderLeft).toBeTruthy();
            spectatorWithHeaderContent.click(tab2HeaderLeft);
            spectatorWithHeaderContent.detectChanges();

            // Both tabs should maintain their header content
            expect(tab1HeaderRight?.querySelector('[data-testid="tab1-header-button"]')).toExist();
            const tab2HeaderRight = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(2) .accordion-header__right'
            );
            expect(tab2HeaderRight?.querySelector('[data-testid="tab2-header-content"]')).toExist();
        });

        it('should handle multiple interactive elements in header content', () => {
            spectatorWithHeaderContent = createComponentWithHeaderContent();

            // Click on span in tab2 header (should not trigger accordion)
            const headerSpan = spectatorWithHeaderContent.query(
                '.accordion-tab:nth-child(2) .accordion-header__right span'
            );
            expect(headerSpan).toBeTruthy();
            spectatorWithHeaderContent.click(headerSpan);
            spectatorWithHeaderContent.detectChanges();

            // Tab should not be active
            const tab2 = spectatorWithHeaderContent.query('.accordion-tab:nth-child(2)');
            expect(tab2).not.toHaveClass('active');

            // Click on button in tab2 header
            const headerButton = spectatorWithHeaderContent.query(
                '[data-testid="tab2-header-button"]'
            );
            expect(headerButton).toBeTruthy();
            spectatorWithHeaderContent.click(headerButton);
            spectatorWithHeaderContent.detectChanges();

            // Button click should be registered
            expect(spectatorWithHeaderContent.query(byTestId('header-button-clicked'))).toHaveText(
                'tab2'
            );

            // Tab should still not be active
            expect(tab2).not.toHaveClass('active');
        });

        it('should render header structure correctly with and without header content', () => {
            spectatorWithHeaderContent = createComponentWithHeaderContent();

            const headers = spectatorWithHeaderContent.queryAll('.accordion-header');

            // All headers should have left and right sections
            headers.forEach((header) => {
                expect(header.querySelector('.accordion-header__left')).toExist();
                expect(header.querySelector('.accordion-header__right')).toExist();
            });

            // Headers with content should have non-empty right sections
            const tab1HeaderRight = headers[0].querySelector('.accordion-header__right');
            const tab2HeaderRight = headers[1].querySelector('.accordion-header__right');
            const tab3HeaderRight = headers[2].querySelector('.accordion-header__right');

            expect(tab1HeaderRight?.children.length).toBeGreaterThan(0);
            expect(tab2HeaderRight?.children.length).toBeGreaterThan(0);
            expect(tab3HeaderRight?.children.length).toBe(0); // No header content for tab3
        });
    });
});

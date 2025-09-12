import { trigger, state, style, transition, animate, AnimationEvent } from '@angular/animations';
import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    input,
    output,
    signal,
    contentChildren,
    AfterViewInit
} from '@angular/core';

import { DotSidebarAccordionTabComponent } from './components/dot-sidebar-accordion-tab/dot-sidebar-accordion-tab.component';

/**
 * DotSidebarAccordion - Reusable accordion component with smooth animations
 *
 * A flexible accordion component that supports multiple tabs with smooth Angular Animations.
 * Uses the same animation techniques as PrimeNG for consistent user experience.
 *
 * @example
 * ```html
 * <dot-sidebar-accordion
 *   [initialActiveTab]="'versions'"
 *   [transitionOptions]="'400ms cubic-bezier(0.86, 0, 0.07, 1)'"
 *   (activeTabChange)="onTabChange($event)">
 *
 *   <dot-sidebar-accordion-tab id="versions" label="Versions">
 *     <div>Version content</div>
 *   </dot-sidebar-accordion-tab>
 *
 *   <dot-sidebar-accordion-tab id="settings" label="Settings">
 *     <div>Settings content</div>
 *   </dot-sidebar-accordion-tab>
 *
 *   <dot-sidebar-accordion-tab id="disabled-tab" label="Disabled" [disabled]="true">
 *     <div>This tab is disabled</div>
 *   </dot-sidebar-accordion-tab>
 *
 * </dot-sidebar-accordion>
 * ```
 *
 * @features
 * - Sequential animations (collapse then expand) for smooth transitions
 * - Pointer events management during transitions
 * - Content projection for flexible tab content
 * - Angular Animations with PrimeNG-compatible timing
 * - Automatic height calculation with `height: '*'`
 * - Customizable animation timing and curves
 * - Individual tab disable/enable functionality
 * - Visual feedback for disabled tabs with muted colors
 *
 * @since 1.0.0
 */
@Component({
    selector: 'dot-sidebar-accordion',
    imports: [CommonModule],
    templateUrl: './dot-sidebar-accordion.component.html',
    styleUrls: ['./dot-sidebar-accordion.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('accordionContent', [
            state(
                'collapsed',
                style({
                    height: '0',
                    opacity: '0',
                    visibility: 'hidden',
                    transform: 'translate3d(0, -4px, 0)' // GPU acceleration
                })
            ),
            state(
                'expanded',
                style({
                    height: '*',
                    opacity: '1',
                    visibility: 'visible',
                    transform: 'translate3d(0, 0, 0)' // GPU acceleration
                })
            ),
            // Optimized transitions with different timing for smooth feel
            transition('expanded => collapsed', [animate('{{collapseTime}} {{collapseEasing}}')]),
            transition('collapsed => expanded', [animate('{{expandTime}} {{expandEasing}}')]),
            transition('void => *', animate(0))
        ])
    ]
})
export class DotSidebarAccordionComponent implements AfterViewInit {
    /**
     * Initial active tab ID
     * @readonly
     */
    $initialActiveTab = input<string>('', { alias: 'initialActiveTab' });

    /**
     * Transition options for animations (optimized PrimeNG-style timing)
     * @readonly
     */
    $transitionOptions = input<string>('150ms cubic-bezier(0.25, 0.8, 0.25, 1)', {
        alias: 'transitionOptions'
    });

    /**
     * Event emitted when active tab changes
     */
    activeTabChange = output<string | null>();

    /**
     * Get all accordion tab children via content projection
     * @readonly
     */
    $tabs = contentChildren(DotSidebarAccordionTabComponent);

    /**
     * Current active tab ID
     */
    $activeTab = signal<string | null>(null);

    /**
     * State for managing smooth transitions with animation callbacks
     */
    $isTransitioning = signal(false);
    private pendingTab: string | null = null;
    private animationStates = new Map<string, 'expanding' | 'collapsing' | 'idle'>();

    /**
     * Computed property to check if there's an active tab
     * @readonly
     */
    $hasActiveTab = computed(() => this.$activeTab() !== null);

    ngAfterViewInit() {
        // Set initial active tab after view init
        const initial = this.$initialActiveTab();
        if (initial) {
            this.$activeTab.set(initial);
        }
    }

    /**
     * Toggle accordion tab with smooth animation callbacks (no setTimeout!)
     *
     * Modern implementation using Angular animation callbacks for perfect timing.
     * Provides smooth sequential transitions without blocking the main thread.
     *
     * @param tabId - The unique identifier of the tab to toggle
     * @public
     */
    toggleTab(tabId: string): void {
        // Check if tab is disabled
        const tab = this.$tabs().find((t) => t.$id() === tabId);
        if (tab?.$disabled()) {
            return; // Don't allow toggle if tab is disabled
        }

        // If already transitioning this specific tab, ignore
        if (
            this.animationStates.get(tabId) !== 'idle' &&
            this.animationStates.get(tabId) !== undefined
        ) {
            return;
        }

        const currentActive = this.$activeTab();
        const targetTab = currentActive === tabId ? null : tabId;

        // If no tab active, expand directly
        if (!currentActive) {
            this.animationStates.set(tabId, 'expanding');
            this.$activeTab.set(targetTab);
            this.activeTabChange.emit(targetTab);
            return;
        }

        // If there's an active tab and we want to change to another, do sequential transition
        if (currentActive && targetTab && currentActive !== targetTab) {
            this.$isTransitioning.set(true);
            this.pendingTab = targetTab;

            // Mark current tab as collapsing
            this.animationStates.set(currentActive, 'collapsing');

            // First collapse the current tab
            this.$activeTab.set(null);
            this.activeTabChange.emit(null);
            // Animation callback will handle the expansion of new tab
        } else {
            // Just collapse the current tab or expand if none active
            if (currentActive) {
                this.animationStates.set(currentActive, 'collapsing');
            }
            this.$activeTab.set(targetTab);
            this.activeTabChange.emit(targetTab);
        }
    }

    /**
     * Handle animation events with perfect timing
     *
     * @param event - Angular animation event
     * @param tabId - The unique identifier of the tab
     */
    onAnimationDone(event: AnimationEvent, tabId: string): void {
        const currentState = this.animationStates.get(tabId);

        if (event.toState === 'collapsed' && currentState === 'collapsing') {
            this.animationStates.set(tabId, 'idle');

            // If there's a pending tab to expand, do it now
            if (this.pendingTab && this.$isTransitioning()) {
                this.animationStates.set(this.pendingTab, 'expanding');
                this.$activeTab.set(this.pendingTab);
                this.activeTabChange.emit(this.pendingTab);
                this.pendingTab = null;
            } else {
                this.$isTransitioning.set(false);
            }
        } else if (event.toState === 'expanded' && currentState === 'expanding') {
            this.animationStates.set(tabId, 'idle');
            this.$isTransitioning.set(false);
        }
    }

    /**
     * Extract duration from transition options string
     *
     * @param transitionOptions - CSS transition string (e.g., '150ms ease')
     * @returns Duration in milliseconds
     */
    private extractAnimationDuration(transitionOptions: string): number {
        const match = transitionOptions.match(/(\d+)ms/);
        return match ? parseInt(match[1], 10) : 200; // Default 200ms
    }

    /**
     * Check if a tab is active
     *
     * @param tabId - The unique identifier of the tab to check
     * @returns True if the tab is currently active
     * @public
     */
    isTabActive(tabId: string): boolean {
        return this.$activeTab() === tabId;
    }

    /**
     * Check if a tab is disabled
     *
     * @param tabId - The unique identifier of the tab to check
     * @returns True if the tab is currently disabled
     * @public
     */
    isTabDisabled(tabId: string): boolean {
        const tab = this.$tabs().find((t) => t.$id() === tabId);
        return tab?.$disabled() ?? false;
    }

    /**
     * Get animation state for a tab with optimized timing parameters
     *
     * @param tabId - The unique identifier of the tab
     * @returns Animation state object with value and optimized timing parameters
     * @public
     */
    getAnimationState(tabId: string) {
        const isActive = this.isTabActive(tabId);
        const baseOptions = this.$transitionOptions();

        // Parse timing for different phases
        const duration = this.extractAnimationDuration(baseOptions);
        const collapseTime = `${Math.round(duration * 0.8)}ms`; // Slightly faster collapse
        const expandTime = `${duration}ms`; // Full duration for expand

        return {
            value: isActive ? 'expanded' : 'collapsed',
            params: {
                collapseTime,
                expandTime,
                collapseEasing: 'cubic-bezier(0.4, 0.0, 1, 1)', // Fast out
                expandEasing: 'cubic-bezier(0.0, 0.0, 0.2, 1)' // Smooth in
            }
        };
    }
}

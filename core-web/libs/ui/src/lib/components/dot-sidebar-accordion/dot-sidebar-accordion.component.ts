import { trigger, state, style, transition, animate } from '@angular/animations';
import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    input,
    inject,
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
 *
 * @since 1.0.0
 */
@Component({
    selector: 'dot-sidebar-accordion',
    standalone: true,
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
                    transform: 'translateY(-8px)'
                })
            ),
            state(
                'expanded',
                style({
                    height: '*',
                    opacity: '1',
                    visibility: 'visible',
                    transform: 'translateY(0)'
                })
            ),
            // Faster collapse for sequential animations
            transition('expanded => collapsed', [animate('{{timing}}')]),
            // Slightly slower expand for smoother feel
            transition('collapsed => expanded', [animate('{{timing}}')]),
            transition('void => *', animate(0))
        ])
    ]
})
export class DotSidebarAccordionComponent implements AfterViewInit {
    private cdr = inject(ChangeDetectorRef);

    /**
     * Initial active tab ID
     * @readonly
     */
    $initialActiveTab = input<string>('', { alias: 'initialActiveTab' });

    /**
     * Transition options for animations (similar to PrimeNG)
     * @readonly
     */
    $transitionOptions = input<string>('200ms cubic-bezier(0.86, 0, 0.07, 1)', {
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
     * State for managing sequential transitions
     */
    isTransitioning = false;
    private pendingTab: string | null = null;

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
            this.cdr.detectChanges();
        }
    }

    /**
     * Toggle accordion tab with sequential smooth animation
     *
     * Handles the logic for expanding/collapsing tabs with smooth transitions.
     * When switching between tabs, it first collapses the current tab, then expands the target tab.
     *
     * @param tabId - The unique identifier of the tab to toggle
     * @public
     */
    toggleTab(tabId: string): void {
        // If already in transition, ignore additional clicks
        if (this.isTransitioning) {
            return;
        }

        const currentActive = this.$activeTab();
        const targetTab = currentActive === tabId ? null : tabId;

        // If no tab active, expand directly
        if (!currentActive) {
            this.$activeTab.set(targetTab);
            this.activeTabChange.emit(targetTab);
            return;
        }

        // If there's an active tab and we want to change to another, do sequential transition
        if (currentActive && targetTab && currentActive !== targetTab) {
            this.isTransitioning = true;
            this.pendingTab = targetTab;

            // First collapse the current tab
            this.$activeTab.set(null);
            this.activeTabChange.emit(null);

            // Wait for collapse animation to complete before expanding new tab
            const collapseTime = this.extractAnimationDuration(this.$transitionOptions());
            setTimeout(() => {
                this.$activeTab.set(this.pendingTab);
                this.activeTabChange.emit(this.pendingTab);
                this.pendingTab = null;
                this.cdr.detectChanges();

                // Wait for expand animation to complete
                setTimeout(() => {
                    this.isTransitioning = false;
                    this.cdr.detectChanges();
                }, collapseTime);
            }, collapseTime);
        } else {
            // Just collapse the current tab or expand if none active
            this.$activeTab.set(targetTab);
            this.activeTabChange.emit(targetTab);
        }
    }

    /**
     * Extract duration from transition options string
     */
    private extractAnimationDuration(transitionOptions: string): number {
        const match = transitionOptions.match(/(\d+)ms/);
        return match ? parseInt(match[1], 10) : 200; // Default 400ms
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
     * Get animation state for a tab
     *
     * @param tabId - The unique identifier of the tab
     * @returns Animation state object with value and timing parameters
     * @public
     */
    getAnimationState(tabId: string) {
        const isActive = this.isTabActive(tabId);
        const baseOptions = this.$transitionOptions();

        return {
            value: isActive ? 'expanded' : 'collapsed',
            params: { timing: baseOptions }
        };
    }
}

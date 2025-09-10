import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, TemplateRef, ViewChild } from '@angular/core';

/**
 * DotSidebarAccordionTab - Individual tab component for content projection
 *
 * This component represents a single tab within the DotSidebarAccordion.
 * It uses ng-template to encapsulate its content for projection.
 *
 * @example
 * ```html
 * <dot-sidebar-accordion-tab id="versions" label="Versions">
 *   <div>Tab content here</div>
 * </dot-sidebar-accordion-tab>
 *
 * <dot-sidebar-accordion-tab id="settings" label="Settings" [disabled]="true">
 *   <div>Disabled tab content</div>
 * </dot-sidebar-accordion-tab>
 * ```
 */
@Component({
    selector: 'dot-sidebar-accordion-tab',
    imports: [CommonModule],
    template: `
        <ng-template #tabContent>
            <ng-content></ng-content>
        </ng-template>
    `,
    styles: [
        `
            :host {
                display: none; /* Tab components are hidden, only their content is projected */
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSidebarAccordionTabComponent {
    /**
     * Unique identifier for the tab
     * @readonly
     */
    $id = input.required<string>({ alias: 'id' });

    /**
     * Display label for the tab header
     * @readonly
     */
    $label = input.required<string>({ alias: 'label' });

    /**
     * Controls whether the tab is disabled
     * @readonly
     */
    $disabled = input<boolean>(false, { alias: 'disabled' });

    /**
     * Template reference for the tab content
     */
    @ViewChild('tabContent', { static: true })
    tabContent!: TemplateRef<unknown>;
}

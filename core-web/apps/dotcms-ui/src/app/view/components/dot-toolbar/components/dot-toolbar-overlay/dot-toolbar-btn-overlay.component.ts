import {
    ChangeDetectionStrategy,
    Component,
    input,
    output,
    signal,
    viewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { Popover, PopoverModule } from 'primeng/popover';

/**
 * A toolbar button component with overlay functionality.
 *
 * This component provides a button that can display an overlay panel when clicked.
 * It supports displaying badges and masks, and manages the overlay panel state.
 *
 * @example
 * ```html
 * <dot-toolbar-btn-overlay
 *   icon="pi pi-bell"
 *   [showBadge]="true">
 *   <ng-container>
 *     <!-- Overlay content goes here -->
 *   </ng-container>
 * </dot-toolbar-btn-overlay>
 * ```
 */
@Component({
    selector: 'dot-toolbar-btn-overlay',
    imports: [ButtonModule, PopoverModule],
    styleUrls: ['./dot-toolbar-btn-overlay.component.scss'],
    templateUrl: 'dot-toolbar-btn-overlay.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotToolbarBtnOverlayComponent {
    /**
     * Signal that controls the visibility of the overlay mask.
     * When true, displays a mask behind the overlay panel.
     */
    $showMask = signal(false);

    /**
     * Input signal that determines whether to show a badge on the button.
     * Typically used to indicate notifications or status.
     *
     * @default false
     */
    $showBadge = input<boolean>(false, { alias: 'showBadge' });

    /**
     * Input signal that determines the style class of the overlay panel.
     *
     * @default ''
     */
    $overlayStyleClass = input<string>('', { alias: 'overlayStyleClass' });

    /**
     * Required input signal for the button icon.
     * Should be a valid PrimeNG icon class (e.g., 'pi pi-bell', 'pi pi-user').
     */
    $icon = input.required<string>({ alias: 'icon' });

    /**
     * ViewChild reference to the PrimeNG Popover component.
     * Used to programmatically control the overlay panel's behavior.
     */
    readonly $overlayPanel = viewChild.required<Popover>('overlayPanel');

    /**
     * Output event emitted when the overlay panel is hidden.
     */
    onHide = output<void>();

    /**
     * Callback method executed when the overlay panel is shown.
     * Sets the mask visibility to true to provide visual feedback.
     */
    handlerShow(): void {
        this.$showMask.set(true);
    }

    /**
     * Callback method executed when the overlay panel is hidden.
     * Sets the mask visibility to false to remove the visual overlay.
     */
    handlerHide(): void {
        this.$showMask.set(false);
        this.onHide.emit();
    }

    /**
     * Hides the overlay panel.
     */
    hide(): void {
        this.$overlayPanel().hide();
    }

    /**
     * Shows the overlay panel.
     * @param event - The event that triggered the show.
     */
    show(event: Event): void {
        this.$overlayPanel().show(event);
    }
}

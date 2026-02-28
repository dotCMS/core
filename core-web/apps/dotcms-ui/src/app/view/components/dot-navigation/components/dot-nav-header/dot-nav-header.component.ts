import { Component, input, inject, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';

import { DotNavLogoService } from '../../../../../api/services/dot-nav-logo/dot-nav-logo.service';

@Component({
    selector: 'dot-nav-header',
    styleUrls: ['./dot-nav-header.component.scss'],
    templateUrl: 'dot-nav-header.component.html',
    imports: [ButtonModule]
})
export class DotNavHeaderComponent {
    /**
     * Service for managing navigation logo data and operations.
     * @private
     */
    readonly #dotNavLogoService = inject(DotNavLogoService);

    /**
     * Event emitter for navigation toggle actions.
     * Emits when the user clicks the toggle button to expand/collapse the sidebar.
     *
     * @emits {void} - Signals that the navigation state should be toggled
     */
    toggle = output<void>();

    /**
     * Signal containing the current navigation logo data.
     * Automatically syncs with the logo service and requires the logo to be available.
     *
     * @readonly
     * @returns {Signal<LogoData>} The current logo configuration and image data
     */
    $logo = toSignal(this.#dotNavLogoService.navBarLogo$, { requireSync: true });

    /**
     * Input signal indicating whether the navigation sidebar is currently collapsed.
     * Used to update the header's visual state and toggle button appearance.
     *
     * @input {boolean} isCollapsed - Current collapsed state of the navigation
     * @required
     */
    $isCollapsed = input.required<boolean>({ alias: 'isCollapsed' });
}

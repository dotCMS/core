import { CommonModule } from '@angular/common';
import { Component, HostBinding, HostListener, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

import { DotNavHeaderComponent } from './components/dot-nav-header/dot-nav-header.component';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotNavigationService } from './services/dot-navigation.service';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

@Component({
    selector: 'dot-main-nav',
    styleUrls: ['./dot-navigation.component.scss'],
    templateUrl: 'dot-navigation.component.html',
    imports: [CommonModule, DotNavHeaderComponent, DotNavItemComponent]
})
export class DotNavigationComponent {
    /**
     * A private readonly instance of `DotNavigationService` injected into the component.
     * This service is used to manage the navigation logic within the application.
     */
    readonly #dotNavigationService = inject(DotNavigationService);

    /**
     * A readonly instance of the IframeOverlayService injected into the component.
     * This service is used to manage the iframe overlay functionality within the application.
     */
    readonly #iframeOverlayService = inject(IframeOverlayService);

    /**
     * Signal representing the menu items from the DotNavigationService.
     *
     * This signal is synchronized with the `items$` observable from the `DotNavigationService`.
     * The `requireSync` option ensures that the signal is updated synchronously with the observable.
     *
     * @type {Signal<MenuItem[]>}
     */
    $menu = toSignal(this.#dotNavigationService.items$, {
        requireSync: true
    });

    /**
     * Signal indicating whether the navigation is collapsed.
     *
     * This signal is synchronized with the `collapsed$` observable from the
     * `DotNavigationService`. It ensures that the state of the navigation
     * (collapsed or expanded) is kept in sync with the service.
     *
     * @type {Signal<boolean>}
     */
    $isCollapsed = toSignal(this.#dotNavigationService.collapsed$, {
        requireSync: true
    });

    @HostBinding('style.overflow-y') get overFlow() {
        return this.$isCollapsed() ? '' : 'auto';
    }

    /**
     * Change or refresh the portlets
     *
     * @param * event click event
     * @param string id menu item id
     * @memberof MainNavigationComponent
     */
    onItemClick($event: { originalEvent: MouseEvent; data: DotMenuItem }): void {
        $event.originalEvent.stopPropagation();

        if (!$event.originalEvent.ctrlKey && !$event.originalEvent.metaKey) {
            this.#dotNavigationService.reloadCurrentPortlet($event.data.id);
            this.#iframeOverlayService.hide();
        }
    }

    /**
     * Open menu with a single click when collapsed
     * otherwise Set isOpen to the passed DotMenu item
     *
     * @param DotMenu currentItem
     * @memberof DotNavigationComponent
     */
    onMenuClick(event: { originalEvent: MouseEvent; data: DotMenu; toggleOnly?: boolean }): void {
        if (this.$isCollapsed()) {
            this.#dotNavigationService.goTo(event.data.menuItems[0].menuLink);
        } else {
            // Check if the menu is not already open to prevent redundant navigation actions.
            if (!event.data.isOpen && !event.toggleOnly) {
                this.#dotNavigationService.goTo(event.data.menuItems[0].menuLink);
            }

            this.#dotNavigationService.setOpen(event.data.id);
        }
    }

    /**
     * Handle click on document to hide the fly-out menu
     *
     * @memberof DotNavItemComponent
     */
    @HostListener('document:click')
    handleDocumentClick(): void {
        if (this.$isCollapsed()) {
            this.#dotNavigationService.closeAllSections();
        }
    }

    /**
     * Handle click on main button to toggle the navigation
     *
     * @memberof DotNavigationComponent
     */
    handleCollapseButtonClick(): void {
        this.#dotNavigationService.toggle();
    }
}

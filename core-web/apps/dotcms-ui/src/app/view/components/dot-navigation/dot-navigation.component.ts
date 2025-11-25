import { Component, HostBinding, HostListener, inject } from '@angular/core';

import { DotEventsService, DotRouterService } from '@dotcms/data-access';
import { DotMenuItem, MenuGroup } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotNavHeaderComponent } from './components/dot-nav-header/dot-nav-header.component';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

@Component({
    selector: 'dot-main-nav',
    styleUrls: ['./dot-navigation.component.scss'],
    templateUrl: 'dot-navigation.component.html',
    imports: [DotNavHeaderComponent, DotNavItemComponent]
})
export class DotNavigationComponent {
    /**
     * A readonly instance of the IframeOverlayService injected into the component.
     * This service is used to manage the iframe overlay functionality within the application.
     */
    readonly #iframeOverlayService = inject(IframeOverlayService);

    /**
     * A readonly instance of the GlobalStore injected into the component.
     * This store provides the menu state signal for rendering the navigation.
     */
    readonly #globalStore = inject(GlobalStore);

    /**
     * A readonly instance of the DotRouterService injected into the component.
     * This service is used for navigation operations.
     */
    readonly #dotRouterService = inject(DotRouterService);

    /**
     * A readonly instance of the DotEventsService injected into the component.
     * This service is used for event notifications.
     */
    readonly #dotEventsService = inject(DotEventsService);

    /**
     * Signal representing the grouped menu items from the GlobalStore menu feature.
     *
     * This signal reads from the computed `menuGroup` state which provides
     * menu items organized by parent with isOpen state.
     *
     * @type {Signal<menuGroup[]>}
     */
    $menu = this.#globalStore.menuGroup;

    /**
     * Signal indicating whether the navigation is collapsed.
     *
     * This signal reads from the `isNavigationCollapsed` state in the menu feature.
     *
     * @type {Signal<boolean>}
     */
    $isCollapsed = this.#globalStore.isNavigationCollapsed;

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
            if (this.#dotRouterService.currentPortlet.id === $event.data.id) {
                this.#dotRouterService.reloadCurrentPortlet($event.data.id);
            } else {
                this.#dotRouterService.gotoPortlet($event.data.menuLink, {
                    queryParams: { mId: $event.data.parentMenuId.substring(0, 4) }
                });
            }

            this.#iframeOverlayService.hide();
        }
    }

    /**
     * Open menu with a single click when collapsed
     * otherwise Set isOpen to the passed MenuGroup item
     *
     * @param MenuGroup currentItem
     * @memberof DotNavigationComponent
     */
    onMenuClick(event: { originalEvent: MouseEvent; data: MenuGroup; toggleOnly?: boolean }): void {
        if (this.$isCollapsed()) {
            this.#dotRouterService.gotoPortlet(event.data.menuItems[0].menuLink, {
                queryParams: { mId: event.data.id.substring(0, 4) }
            });
        } else {
            // Check if the menu is not already open to prevent redundant navigation actions.
            if (!event.data.isOpen && !event.toggleOnly) {
                this.#dotRouterService.gotoPortlet(event.data.menuItems[0].menuLink, {
                    queryParams: { mId: event.data.id.substring(0, 4) }
                });
            }

            this.#globalStore.toggleParent(event.data.id);
        }
    }

    /**
     * Handle click on main button to toggle the navigation
     *
     * @memberof DotNavigationComponent
     */
    handleCollapseButtonClick(): void {
        this.#dotEventsService.notify('dot-side-nav-toggle');
        this.#globalStore.toggleNavigation();
    }
}

import { Component, HostListener, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import { DotMenu, DotMenuItem } from '@models/navigation';
import { DotNavigationService } from './services/dot-navigation.service';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';

@Component({
    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['./dot-navigation.component.scss'],
    templateUrl: 'dot-navigation.component.html'
})
export class DotNavigationComponent implements OnInit {
    menu$: Observable<DotMenu[]>;

    constructor(
        public dotNavigationService: DotNavigationService,
        public iframeOverlayService: IframeOverlayService
    ) {}

    ngOnInit() {
        this.menu$ = this.dotNavigationService.items$;
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
            this.dotNavigationService.reloadCurrentPortlet($event.data.id);
        }
    }

    /**
     * Open menu with a single click when collapsed
     * otherwise Set isOpen to the passed DotMenu item
     *
     * @param DotMenu currentItem
     * @memberof DotNavigationComponent
     */
    onMenuClick(event: { originalEvent: MouseEvent; data: DotMenu }): void {
        if (this.dotNavigationService.collapsed$.getValue()) {
            this.dotNavigationService.goTo(event.data.menuItems[0].menuLink);
        } else {
            this.dotNavigationService.setOpen(event.data.id);
        }
    }

    /**
     * Set isOpen to the passed DotMenu item
     *
     * @param DotMenu currentItem
     * @memberof DotNavigationComponent
     */
    onMenuRightClick(event: { originalEvent: MouseEvent; data: DotMenu }): void {
        this.dotNavigationService.setOpen(event.data.id);
        this.iframeOverlayService.show();
    }

    /**
     * Handle click on document to hide the fly-out menu
     *
     * @memberof DotNavItemComponent
     */
    @HostListener('document:click')
    handleDocumentClick(): void {
        if (this.dotNavigationService.collapsed$.getValue()) {
            this.dotNavigationService.closeAllSections();
        }
    }
}

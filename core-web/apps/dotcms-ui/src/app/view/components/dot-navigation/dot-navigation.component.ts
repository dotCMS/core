import { Observable } from 'rxjs';

import { Component, HostBinding, HostListener, OnInit } from '@angular/core';

import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

import { DotNavigationService } from './services/dot-navigation.service';

@Component({
    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['./dot-navigation.component.scss'],
    templateUrl: 'dot-navigation.component.html'
})
export class DotNavigationComponent implements OnInit {
    menu$: Observable<DotMenu[]>;

    @HostBinding('style.overflow-y') get overFlow() {
        return this.dotNavigationService.collapsed$.getValue() ? '' : 'auto';
    }

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
            this.iframeOverlayService.hide();
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

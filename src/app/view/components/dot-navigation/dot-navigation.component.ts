import { Component, OnInit, Input, EventEmitter, Output, OnChanges, SimpleChanges } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { DotMenu, DotMenuItem } from '../../../shared/models/navigation';
import { DotNavigationService } from './services/dot-navigation.service';

@Component({
    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['./dot-navigation.component.scss'],
    templateUrl: 'dot-navigation.component.html'
})
export class DotNavigationComponent implements OnInit, OnChanges {
    @Input() collapsed = false;
    @Output() change = new EventEmitter<boolean>();
    menu$: Observable<DotMenu[]>;

    constructor(private dotNavigationService: DotNavigationService) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (!changes.collapsed.firstChange) {
            changes.collapsed.currentValue ? this.dotNavigationService.collapseMenu() : this.dotNavigationService.expandMenu();
        }
    }

    ngOnInit() {
        this.menu$ = this.dotNavigationService.items$;
    }

    /**
     * Change or refresh the portlets
     *
     * @param {*} event click event
     * @param {string} id menu item id
     * @memberof MainNavigationComponent
     */
    onItemClick($event: { originalEvent: MouseEvent; data: DotMenuItem }): void {
        $event.originalEvent.stopPropagation();

        if (!$event.originalEvent.ctrlKey && !$event.originalEvent.metaKey) {
            this.dotNavigationService.reloadCurrentPortlet($event.data.id);
        }
    }

    /**
     * Set isOpen to the passed DotMenu item
     *
     * @param {DotMenu} currentItem
     * @memberof DotNavigationComponent
     */
    onMenuClick(event: { originalEvent: MouseEvent; data: DotMenu }): void {
        this.change.emit();

        if (this.collapsed) {
            this.dotNavigationService.goTo(event.data.menuItems[0].menuLink);
        }

        this.dotNavigationService.setOpen(event.data.id);
    }
}

import { Component, OnInit, Input, OnChanges, SimpleChanges, EventEmitter, Output, SimpleChange } from '@angular/core';
import { NavigationEnd } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';
import { take, retryWhen, concatMap } from 'rxjs/operators';

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
    menu: DotMenu[];

    constructor(private dotNavigationService: DotNavigationService) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (!changes.collapsed.firstChange) {
            this.menu = this.menu.map((item: DotMenu) => {
                item.isOpen = this.shouldOpenMenuWhenUncollapse(changes.collapsed, item);
                return item;
            });
        }
    }

    ngOnInit() {
        this.dotNavigationService.items$
            .pipe(
                concatMap((menu: DotMenu[]) => (menu.length ? of(menu) : Observable.throw('Nav is not ready'))),
                retryWhen(() => Observable.timer(100)),
                take(1)
            )
            .subscribe((menu: DotMenu[]) => {
                this.menu = menu;
            });

        this.dotNavigationService.onNavigationEnd().subscribe((event: NavigationEnd) => {
            const urlSegments: string[] = event.url.split('/');

            if (this.shouldUpdateActiveState(urlSegments)) {
                this.setActive(urlSegments.pop());
            }
        });
    }

    /**
     * Change or refresh the portlets
     *
     * @param {*} event click event
     * @param {string} id menu item id
     * @memberof MainNavigationComponent
     */
    onClick($event: { originalEvent: MouseEvent; data: DotMenuItem }): void {
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

        this.menu = this.menu.map((item: DotMenu) => {
            item.isOpen = item.isOpen ? false : event.data.id === item.id;
            return item;
        });
    }

    private getActiveUpdatedMenu(menu: DotMenu, id: string): DotMenu {
        let isActive = false;

        menu.menuItems.forEach((item: DotMenuItem) => {
            if (item.id === id) {
                item.active = true;
                isActive = true;
            } else {
                item.active = false;
            }
        });

        menu.active = isActive;
        menu.isOpen = menu.active;

        return menu;
    }

    private setActive(id: string) {
        this.menu = this.menu.map((item: DotMenu) => this.getActiveUpdatedMenu(item, id));
    }

    private shouldOpenMenuWhenUncollapse(collapsed: SimpleChange, item: DotMenu): boolean {
        return !collapsed.currentValue && item.active;
    }

    private shouldUpdateActiveState(urlSegments: string[]): boolean {
        const blackList = ['edit-page'];
        const isBlackListed = !!urlSegments.filter(element => blackList.includes(element)).length;
        return urlSegments.length < 4 && !isBlackListed;
    }
}

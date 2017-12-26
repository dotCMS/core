import { DotNavigationComponent } from './../dot-navigation/dot-navigation.component';
import {Component, OnDestroy, OnInit, ViewEncapsulation, HostListener } from '@angular/core';
import { DotEventsService } from '../../../api/services/dot-events/dot-events.service';
import { Router } from '@angular/router';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['./main-legacy.component.scss'],
    templateUrl: './main-legacy.component.html'
})
export class MainComponentLegacy implements OnInit, OnDestroy {
    isMenuCollapsed = false;
    isTablet = false;
    private messages: any = {};
    private label = '';

    constructor(private dotEventsService: DotEventsService, private router: Router) {}

    /**
     * Set isTablet when resizing the window size
     * @param {any} event
     * @memberof MainComponentLegacy
     */
    @HostListener('window:resize', ['$event'])
        onResize(event: any) {
        this.isTablet = event.target.innerWidth < 1025;
    }

    /**
     * Respond to document events and collapse the sidenav if is clicked outside
     * @param {*} event
     * @memberof DotNavigationComponent
     */
    @HostListener('click', ['$event'])
    onClickOutside(event: any) {
        if (this.isTablet && !this.isMenuCollapsed) {
            this.isMenuCollapsed = true;
        }
    }

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
        this.router.events.subscribe(event => this.setMenuState());
        this.setMenuState();
    }

    ngOnDestroy(): void {
        this.messages = null;
        this.label = null;
    }

    /**
     * Set collapsed menu state base on the screen size
     * @memberof MainComponentLegacy
     */
    setMenuState(): void {
        if (window.innerWidth < 1025) {
            this.isTablet = true;
            this.isMenuCollapsed = true;
        }
    }

    /**
     * Toggle show/hide sidenav
     *
     * @memberof MainComponentLegacy
     */
    toggleSidenav(): void {
        this.isMenuCollapsed = !this.isMenuCollapsed;
        this.dotEventsService.notify('dot-side-nav-toggle');
    }
}

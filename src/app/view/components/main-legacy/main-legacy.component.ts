import { DotAlertConfirmService } from '../../../api/services/dot-alert-confirm/dot-alert-confirm.service';
import { Component, OnInit, ViewEncapsulation, HostListener } from '@angular/core';
import { DotEventsService } from '../../../api/services/dot-events/dot-events.service';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators/filter';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { DotIframeService } from '../_common/iframe/service/dot-iframe/dot-iframe.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['./main-legacy.component.scss'],
    templateUrl: './main-legacy.component.html'
})
export class MainComponentLegacyComponent implements OnInit {
    isMenuCollapsed = false;
    isTablet = false;

    constructor(
        private dotEventsService: DotEventsService,
        private router: Router,
        private dotRouterService: DotRouterService,
        private dotIframeService: DotIframeService,
    ) {}

    /**
     * Set isTablet when resizing the window size
     *
     * @param {*} event
     * @memberof MainComponentLegacyComponent
     */
    @HostListener('window:resize', ['$event'])
    onResize(event: any) {
        this.isTablet = event.target.innerWidth < 1025;
    }

    /**
     * Respond to document events and collapse the sidenav if is clicked outside
     *
     * @param {*} _event
     * @memberof MainComponentLegacyComponent
     */
    @HostListener('click', ['$event'])
    onClickOutside(_event: any) {
        if (this.isTablet && !this.isMenuCollapsed) {
            this.isMenuCollapsed = true;
        }
    }

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
        this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe((_event) => this.setMenuState());
        this.setMenuState();
    }

    /**
     * Reload content search iframe when contentlet editor close
     *
     * @memberof MainComponentLegacyComponent
     */
    onCloseContentletEditor(): void {
        this.dotIframeService.reloadData(this.dotRouterService.currentPortlet.id);
    }

    /**
     * Set collapsed menu state base on the screen size
     *
     * @memberof MainComponentLegacyComponent
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
     * @memberof MainComponentLegacyComponent
     */
    toggleSidenav(): void {
        this.isMenuCollapsed = !this.isMenuCollapsed;
        this.dotEventsService.notify('dot-side-nav-toggle');
    }


}

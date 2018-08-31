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

    constructor(
        private dotEventsService: DotEventsService,
        private router: Router,
        private dotRouterService: DotRouterService,
        private dotIframeService: DotIframeService,
    ) {}

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
    }

    /**
     * When navigation change gets handled
     *
     * @memberof MainComponentLegacyComponent
     */
    handleMenuChange(): void {
        if (this.isMenuCollapsed) {
            this.isMenuCollapsed = false;
            this.dotEventsService.notify('dot-side-nav-toggle');
        }
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
     * Toggle show/hide sidenav
     *
     * @memberof MainComponentLegacyComponent
     */
    toggleSidenav(): void {
        this.isMenuCollapsed = !this.isMenuCollapsed;
        this.dotEventsService.notify('dot-side-nav-toggle');
    }
}

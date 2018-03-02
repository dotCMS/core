import { DotDialogService } from '../../../api/services/dot-dialog/dot-dialog.service';
import { Component, OnInit, ViewEncapsulation, HostListener } from '@angular/core';
import { DotEventsService } from '../../../api/services/dot-events/dot-events.service';
import { Router } from '@angular/router';

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

    constructor(private dotEventsService: DotEventsService, private router: Router,  public dotDialotService: DotDialogService) {}

    /**
     * Set isTablet when resizing the window size
     * @param {any} event
     * @memberof MainComponentLegacyComponentComponent
     */
    @HostListener('window:resize', ['$event'])
    onResize(event: any) {
        this.isTablet = event.target.innerWidth < 1025;
    }

    /**
     * Respond to document events and collapse the sidenav if is clicked outside
     * @param {*} event
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
        this.router.events.subscribe((_event) => this.setMenuState());
        this.setMenuState();
    }

    /**
     * Set collapsed menu state base on the screen size
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

import { BehaviorSubject } from 'rxjs';

import { Component, inject, Input, OnInit } from '@angular/core';

import { DotNavLogoService } from '@dotcms/app/api/services/dot-nav-logo/dot-nav-logo.service';
import { DotRouterService } from '@dotcms/data-access';
import { DotcmsEventsService, Site, SiteService } from '@dotcms/dotcms-js';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';

@Component({
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html'
})
export class DotToolbarComponent implements OnInit {
    readonly #dotNavLogoService = inject(DotNavLogoService);

    @Input()
    collapsed: boolean;
    logo$: BehaviorSubject<string> = this.#dotNavLogoService.navBarLogo$;

    constructor(
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private siteService: SiteService,
        public dotNavigationService: DotNavigationService,
        public iframeOverlayService: IframeOverlayService
    ) {}

    ngOnInit(): void {
        this.dotcmsEventsService.subscribeTo<Site>('ARCHIVE_SITE').subscribe((data: Site) => {
            if (data.hostname === this.siteService.currentSite.hostname && data.archived) {
                this.siteService.switchToDefaultSite().subscribe((defaultSite: Site) => {
                    this.siteChange(defaultSite);
                });
            }
        });
    }

    siteChange(site: Site): void {
        this.siteService.switchSite(site).subscribe(() => {
            // wait for the site to be switched
            // before redirecting to the site browser
            if (this.dotRouterService.isEditPage()) {
                this.dotRouterService.goToSiteBrowser();
            }
        });
    }

    handleMainButtonClick(): void {
        this.dotNavigationService.toggle();
    }
}

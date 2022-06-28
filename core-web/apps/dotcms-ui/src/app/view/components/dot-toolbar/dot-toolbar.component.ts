import { Component, Input, OnInit } from '@angular/core';
import { SiteService, Site, DotcmsEventsService, DotcmsConfigService } from '@dotcms/dotcms-js';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';
import { DotNavLogoService } from '@services/dot-nav-logo/dot-nav-logo.service';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html'
})
export class DotToolbarComponent implements OnInit {
    @Input()
    collapsed: boolean;
    logo$: BehaviorSubject<string> = this.dotNavLogoService.navBarLogo$;

    constructor(
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private dotCmsConfigService: DotcmsConfigService,
        private dotNavLogoService: DotNavLogoService,
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
        this.siteService.switchSite(site);

        if (this.dotRouterService.isEditPage()) {
            this.dotRouterService.goToSiteBrowser();
        }
    }

    handleMainButtonClick(): void {
        this.dotNavigationService.toggle();
    }
}

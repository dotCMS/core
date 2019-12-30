import { Component, Input, OnInit } from '@angular/core';
import { SiteService, Site, DotcmsEventsService } from 'dotcms-js';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';

@Component({
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html'
})
export class DotToolbarComponent implements OnInit {
    @Input()
    collapsed: boolean;

    constructor(
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private siteService: SiteService,
        public dotNavigationService: DotNavigationService,
        public iframeOverlayService: IframeOverlayService
    ) {}

    ngOnInit(): void {
        this.dotcmsEventsService
            .subscribeTo<Site>('ARCHIVE_SITE')
            .subscribe((data: Site) => {
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

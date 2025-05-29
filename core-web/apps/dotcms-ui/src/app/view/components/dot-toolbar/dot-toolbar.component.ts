import { BehaviorSubject } from 'rxjs';

import { NgStyle, AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotCrumbtrailComponent } from '@components/dot-crumbtrail/dot-crumbtrail.component';
import { DotNavLogoService } from '@dotcms/app/api/services/dot-nav-logo/dot-nav-logo.service';
import { DotRouterService } from '@dotcms/data-access';
import { DotcmsEventsService, Site, SiteService } from '@dotcms/dotcms-js';

import { DotToolbarNotificationsComponent } from './components/dot-toolbar-notifications/dot-toolbar-notifications.component';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';

@Component({
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html',
    standalone: true,
    imports: [
        ToolbarModule,
        ButtonModule,
        NgStyle,
        DotCrumbtrailComponent,
        DotSiteSelectorModule,
        DotToolbarNotificationsComponent,
        DotToolbarUserComponent,
        AsyncPipe
    ]
})
export class DotToolbarComponent implements OnInit {
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotcmsEventsService = inject(DotcmsEventsService);
    readonly #siteService = inject(SiteService);
    readonly #dotNavLogoService = inject(DotNavLogoService);
    readonly iframeOverlayService = inject(IframeOverlayService);
    readonly dotNavigationService = inject(DotNavigationService);

    logo$: BehaviorSubject<string> = this.#dotNavLogoService.navBarLogo$;

    ngOnInit(): void {
        this.#dotcmsEventsService.subscribeTo<Site>('ARCHIVE_SITE').subscribe((data: Site) => {
            if (data.hostname === this.#siteService.currentSite.hostname && data.archived) {
                this.#siteService.switchToDefaultSite().subscribe((defaultSite: Site) => {
                    this.siteChange(defaultSite);
                });
            }
        });
    }

    siteChange(site: Site): void {
        this.#siteService.switchSite(site).subscribe(() => {
            // wait for the site to be switched
            // before redirecting to the site browser
            if (this.#dotRouterService.isEditPage()) {
                this.#dotRouterService.goToSiteBrowser();
            }
        });
    }

    handleMainButtonClick(): void {
        this.dotNavigationService.toggle();
    }
}

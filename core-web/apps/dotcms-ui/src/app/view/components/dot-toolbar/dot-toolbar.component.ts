import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { DotRouterService } from '@dotcms/data-access';
import { DotcmsEventsService, Site, SiteService } from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotToolbarAnnouncementsComponent } from './components/dot-toolbar-announcements/dot-toolbar-announcements.component';
import { DotToolbarNotificationModule } from './components/dot-toolbar-notifications/dot-toolbar-notifications.module';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';

import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotSiteSelectorComponent } from '../_common/dot-site-selector/dot-site-selector.component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotCrumbtrailModule } from '../dot-crumbtrail/dot-crumbtrail.module';

@Component({
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html',
    imports: [
        CommonModule,
        ToolbarModule,
        DividerModule,
        DotCrumbtrailModule,
        DotSiteSelectorComponent,
        DotToolbarNotificationModule,
        DotToolbarAnnouncementsComponent,
        DotToolbarUserComponent,
        DotShowHideFeatureDirective
    ]
})
export class DotToolbarComponent implements OnInit {
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotcmsEventsService = inject(DotcmsEventsService);
    readonly #siteService = inject(SiteService);
    readonly #destroyRef = inject(DestroyRef);
    iframeOverlayService = inject(IframeOverlayService);

    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;

    ngOnInit(): void {
        this.#dotcmsEventsService
            .subscribeTo<Site>('ARCHIVE_SITE')
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((data: Site) => {
                if (data.hostname === this.#siteService.currentSite.hostname && data.archived) {
                    this.#siteService.switchToDefaultSite().subscribe((defaultSite: Site) => {
                        this.siteChange(defaultSite);
                    });
                }
            });
    }

    siteChange(site: Site): void {
        this.#siteService
            .switchSite(site)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                // wait for the site to be switched
                // before redirecting to the site browser
                if (this.#dotRouterService.isEditPage()) {
                    this.#dotRouterService.goToSiteBrowser();
                }
            });
    }
}

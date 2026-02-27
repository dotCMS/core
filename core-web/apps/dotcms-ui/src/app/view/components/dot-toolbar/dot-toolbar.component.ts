import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { DotRouterService } from '@dotcms/data-access';
import { DotcmsEventsService, Site, SiteService } from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotToolbarAnnouncementsComponent } from './components/dot-toolbar-announcements/dot-toolbar-announcements.component';
import { DotToolbarNotificationsComponent } from './components/dot-toolbar-notifications/dot-toolbar-notifications.component';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';

import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotSiteSelectorComponent } from '../_common/dot-site-selector/dot-site-selector.component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotCrumbtrailComponent } from '../dot-crumbtrail/dot-crumbtrail.component';

@Component({
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html',
    imports: [
        ToolbarModule,
        DividerModule,
        DotCrumbtrailComponent,
        DotSiteSelectorComponent,
        DotToolbarNotificationsComponent,
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
    readonly #http = inject(HttpClient);
    iframeOverlayService = inject(IframeOverlayService);

    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;
    serverGreeting = signal<string>('');
    serverUptime = signal<string>('');

    ngOnInit(): void {
        this.#http
            .get<{ entity: { message: string; serverTime: string; uptimeSeconds: string } }>('/api/v1/demo/greet')
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (response) => {
                    this.serverGreeting.set(response.entity.message);
                    const secs = parseInt(response.entity.uptimeSeconds, 10);
                    const h = Math.floor(secs / 3600);
                    const m = Math.floor((secs % 3600) / 60);
                    const s = secs % 60;
                    this.serverUptime.set(`up ${h}h ${m}m ${s}s`);
                },
                error: () => {
                    this.serverGreeting.set('');
                    this.serverUptime.set('');
                }
            });

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

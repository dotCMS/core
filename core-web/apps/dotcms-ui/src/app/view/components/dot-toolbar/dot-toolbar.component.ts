import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { switchMap, take } from 'rxjs/operators';

import { DotRouterService, DotSiteService } from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotSite, FeaturedFlags } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotSiteComponent } from '@dotcms/ui';

import { DotToolbarAnnouncementsComponent } from './components/dot-toolbar-announcements/dot-toolbar-announcements.component';
import { DotToolbarNotificationsComponent } from './components/dot-toolbar-notifications/dot-toolbar-notifications.component';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';

import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotCrumbtrailComponent } from '../dot-crumbtrail/dot-crumbtrail.component';

@Component({
    selector: 'dot-toolbar',
    templateUrl: './dot-toolbar.component.html',
    imports: [
        ToolbarModule,
        DividerModule,
        DotCrumbtrailComponent,
        DotToolbarNotificationsComponent,
        DotToolbarAnnouncementsComponent,
        DotToolbarUserComponent,
        DotShowHideFeatureDirective,
        DotSiteComponent,
        FormsModule
    ]
})
export class DotToolbarComponent implements OnInit {
    readonly globalStore = inject(GlobalStore);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotcmsEventsService = inject(DotcmsEventsService);
    readonly #siteService = inject(DotSiteService);
    readonly #destroyRef = inject(DestroyRef);
    iframeOverlayService = inject(IframeOverlayService);

    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;

    ngOnInit(): void {
        this.#dotcmsEventsService
            .subscribeTo<DotSite>('ARCHIVE_SITE')
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((archivedSite: DotSite) => {
                if (archivedSite.identifier !== this.globalStore.siteDetails()?.identifier) {
                    return;
                }

                // Current site was archived — backend auto-switches; fetch the new current site
                this.#siteService
                    .getCurrentSite()
                    .pipe(take(1))
                    .subscribe((site) => this.globalStore.setCurrentSite(site));
            });
    }

    siteChange(identifier: string | null): void {
        if (identifier) {
            this.#siteService
                .switchSite(identifier)
                .pipe(
                    switchMap(() => this.#siteService.getCurrentSite()),
                    take(1),
                    takeUntilDestroyed(this.#destroyRef)
                )
                .subscribe((site: DotSite) => {
                    if (this.#dotRouterService.isEditPage()) {
                        this.#dotRouterService.goToSiteBrowser();
                    }
                    this.globalStore.setCurrentSite(site);
                });
        }
    }
}

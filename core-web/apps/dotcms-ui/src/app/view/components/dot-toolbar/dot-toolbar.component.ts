import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { switchMap, take } from 'rxjs/operators';

import { DotRouterService, DotSiteService } from '@dotcms/data-access';
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
    #globalStore = inject(GlobalStore);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #siteService = inject(DotSiteService);
    readonly #destroyRef = inject(DestroyRef);
    iframeOverlayService = inject(IframeOverlayService);

    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;

    $currentSite = this.#globalStore.siteDetails;

    ngOnInit(): void {
        // When another user/tab switches the site, update the store and navigate away from edit page
        this.#globalStore
            .switchSiteEvent$()
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((site: DotSite) => {
                this.#globalStore.setCurrentSite(site);
                if (this.#dotRouterService.isEditPage()) {
                    this.#dotRouterService.goToSiteBrowser();
                }
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
                    this.#globalStore.setCurrentSite(site);
                });
        }
    }
}

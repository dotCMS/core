import { Component, DestroyRef, OnInit, Signal, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { map, switchMap, take } from 'rxjs/operators';

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
    #globalStore = inject(GlobalStore);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotcmsEventsService = inject(DotcmsEventsService);
    readonly #siteService = inject(DotSiteService);
    readonly #destroyRef = inject(DestroyRef);
    iframeOverlayService = inject(IframeOverlayService);

    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;

    $currentSite: Signal<DotSite | null> = toSignal(this.#siteService.getCurrentSite());

    ngOnInit(): void {
        this.#dotcmsEventsService
            .subscribeTo<DotSite>('ARCHIVE_SITE')
            .pipe(
                switchMap((data: DotSite) =>
                    this.#siteService.getCurrentSite().pipe(
                        take(1),
                        map((currentSite: DotSite) => ({ data, currentSite }))
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ data, currentSite }) => {
                if (data.hostname === currentSite.hostname && data.archived) {
                    this.#siteService.switchSite(null).subscribe((defaultSite: DotSite) => {
                        this.siteChange(defaultSite.identifier);
                    });
                }
            });
    }

    siteChange(identifier: string | null): void {
        if (identifier) {
            this.#siteService
                .switchSite(identifier)
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((site: DotSite) => {
                    // wait for the site to be switched
                    // before redirecting to the site browser
                    if (this.#dotRouterService.isEditPage()) {
                        this.#dotRouterService.goToSiteBrowser();
                    }
                    this.#globalStore.setCurrentSite(site);
                });
        }
    }
}

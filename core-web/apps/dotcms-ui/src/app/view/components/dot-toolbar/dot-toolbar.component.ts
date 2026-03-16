import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { FeaturedFlags } from '@dotcms/dotcms-models';
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
export class DotToolbarComponent {
    readonly #globalStore = inject(GlobalStore);
    iframeOverlayService = inject(IframeOverlayService);

    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;

    $currentSite = this.#globalStore.siteDetails;

    siteChange(identifier: string | null): void {
        if (identifier) {
            this.#globalStore.switchCurrentSite(identifier);
        }
    }
}

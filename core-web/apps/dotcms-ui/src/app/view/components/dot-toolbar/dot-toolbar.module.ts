import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotCrumbtrailModule } from '@components/dot-crumbtrail/dot-crumbtrail.module';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';

import { DotToolbarAnnouncementsComponent } from './components/dot-toolbar-announcements/dot-toolbar-announcements.component';
import { DotToolbarNotificationModule } from './components/dot-toolbar-notifications/dot-toolbar-notifications.module';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';
import { DotToolbarComponent } from './dot-toolbar.component';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotCrumbtrailModule,
        DotSiteSelectorModule,
        DotToolbarNotificationModule,
        DotToolbarAnnouncementsComponent,
        ToolbarModule,
        DotToolbarUserComponent,
        DotShowHideFeatureDirective,
        DividerModule
    ],
    declarations: [DotToolbarComponent],
    exports: [DotToolbarComponent]
})
export class DotToolbarModule {}

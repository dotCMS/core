import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotCustomTimeModule } from '@components/_common/dot-custom-time.component/dot-custom-time.module';
import { DotDropdownModule } from '@components/_common/dot-dropdown-component/dot-dropdown.module';
import { AnnouncementsStore } from '@components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotNotificationItemComponent } from './components/dot-notification-item/dot-notification-item.component';
import { DotNotificationListComponent } from './components/dot-notification-list/dot-notification-list.component';
import { DotNotificationsComponent } from './components/dot-notifications/dot-notifications.component';
import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';

import { DotToolbarAnnouncementsComponent } from '../dot-toolbar-announcements/dot-toolbar-announcements.component';

@NgModule({
    imports: [
        CommonModule,
        DotDropdownModule,
        ButtonModule,
        DotCustomTimeModule,
        DotSafeHtmlPipe,
        DotMessagePipe,
        DividerModule,
        DotToolbarAnnouncementsComponent,
        DotShowHideFeatureDirective,
        DotNotificationItemComponent,
        DotNotificationListComponent,
        BadgeModule,
        OverlayPanelModule,
        DotNotificationsComponent
    ],
    exports: [DotToolbarNotificationsComponent],
    declarations: [DotToolbarNotificationsComponent],
    providers: [AnnouncementsStore]
})
export class DotToolbarNotificationModule {}

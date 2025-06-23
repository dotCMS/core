import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';

import { DotCustomTimeModule } from '@components/_common/dot-custom-time.component/dot-custom-time.module';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotNotificationItemComponent } from './components/dot-notification-item/dot-notification-item.component';
import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';

import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        DotCustomTimeModule,
        DotSafeHtmlPipe,
        DotMessagePipe,
        DividerModule,
        DotNotificationItemComponent,
        DotToolbarBtnOverlayComponent
    ],
    exports: [DotToolbarNotificationsComponent],
    declarations: [DotToolbarNotificationsComponent]
})
export class DotToolbarNotificationModule {}

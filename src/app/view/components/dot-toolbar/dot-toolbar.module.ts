import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotCrumbtrailModule } from '@components/dot-crumbtrail/dot-crumbtrail.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotToolbarComponent } from './dot-toolbar.component';
import { DotToolbarNotificationModule } from './components/dot-toolbar-notifications/dot-toolbar-notifications.module';
import { DotToolbarUserModule } from './components/dot-toolbar-user/dot-toolbar-user.module';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotCrumbtrailModule,
        DotIconButtonModule,
        DotSiteSelectorModule,
        DotToolbarNotificationModule,
        DotToolbarUserModule,
        ToolbarModule
    ],
    declarations: [DotToolbarComponent],
    exports: [DotToolbarComponent]
})
export class DotToolbarModule {}

import { CommonModule } from '@angular/common';
import { HttpClientJsonpModule } from '@angular/common/http';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotCrumbtrailModule } from '@components/dot-crumbtrail/dot-crumbtrail.module';
import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';

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
        ToolbarModule,
        DotToolbarUserComponent,
        HttpClientJsonpModule
    ],
    declarations: [DotToolbarComponent],
    exports: [DotToolbarComponent],
    providers: [DotGravatarService]
})
export class DotToolbarModule {}

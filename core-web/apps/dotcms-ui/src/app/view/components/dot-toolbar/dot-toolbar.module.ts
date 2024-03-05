import { CommonModule } from '@angular/common';
import { HttpClientJsonpModule } from '@angular/common/http';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotToolbarNotificationModule } from './components/dot-toolbar-notifications/dot-toolbar-notifications.module';
import { DotToolbarUserComponent } from './components/dot-toolbar-user/dot-toolbar-user.component';
import { DotToolbarComponent } from './dot-toolbar.component';

import { DotGravatarService } from '../../../api/services/dot-gravatar-service';
import { DotSiteSelectorModule } from '../_common/dot-site-selector/dot-site-selector.module';
import { DotCrumbtrailModule } from '../dot-crumbtrail/dot-crumbtrail.module';

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

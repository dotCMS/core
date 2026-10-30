import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import {
    DotCopyButtonComponent,
    DotKeyValueComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotAppsConfigurationDetailFormModule } from './dot-apps-configuration-detail-form/dot-apps-configuration-detail-form.module';
import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationDetailComponent } from './dot-apps-configuration-detail.component';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';
import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotKeyValueComponent,
        DotCopyButtonComponent,
        DotAppsConfigurationHeaderModule,
        DotAppsConfigurationDetailFormModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    declarations: [DotAppsConfigurationDetailComponent],
    exports: [DotAppsConfigurationDetailComponent],
    providers: [DotAppsService, DotAppsConfigurationDetailResolver]
})
export class DotAppsConfigurationDetailModule {}

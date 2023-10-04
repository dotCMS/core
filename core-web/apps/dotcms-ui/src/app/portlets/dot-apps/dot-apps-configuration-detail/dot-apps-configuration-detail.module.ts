import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotKeyValueModule } from '@components/dot-key-value-ng/dot-key-value-ng.module';
import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationDetailFormModule } from './dot-apps-configuration-detail-form/dot-apps-configuration-detail-form.module';
import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationDetailComponent } from './dot-apps-configuration-detail.component';

import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotKeyValueModule,
        DotCopyButtonComponent,
        DotAppsConfigurationHeaderModule,
        DotAppsConfigurationDetailFormModule,
        DotPipesModule,
        DotMessagePipe
    ],
    declarations: [DotAppsConfigurationDetailComponent],
    exports: [DotAppsConfigurationDetailComponent],
    providers: [DotAppsService, DotAppsConfigurationDetailResolver]
})
export class DotAppsConfigurationDetailModule {}

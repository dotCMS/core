import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';

import { DotCopyLinkModule } from '../../../../../view/components/dot-copy-link/dot-copy-link.module';

@NgModule({
    imports: [
        CommonModule,
        DotCopyLinkModule,
        TooltipModule,
        DotMessagePipe,
        ButtonModule,
        DotSafeHtmlPipe
    ],
    declarations: [DotAppsConfigurationItemComponent],
    exports: [DotAppsConfigurationItemComponent]
})
export class DotAppsConfigurationItemModule {}

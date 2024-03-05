import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotAppsConfigurationHeaderComponent } from './dot-apps-configuration-header.component';

import { DotCopyLinkModule } from '../../../view/components/dot-copy-link/dot-copy-link.module';
import { DotPipesModule } from '../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        AvatarModule,
        DotAvatarDirective,
        DotCopyLinkModule,
        DotPipesModule,
        MarkdownModule.forChild(),
        DotMessagePipe
    ],
    declarations: [DotAppsConfigurationHeaderComponent],
    exports: [DotAppsConfigurationHeaderComponent]
})
export class DotAppsConfigurationHeaderModule {}

import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationHeaderComponent } from './dot-apps-configuration-header.component';

@NgModule({
    imports: [
        CommonModule,
        DotAvatarModule,
        DotCopyLinkModule,
        DotPipesModule,
        MarkdownModule.forChild()
    ],
    declarations: [DotAppsConfigurationHeaderComponent],
    exports: [DotAppsConfigurationHeaderComponent]
})
export class DotAppsConfigurationHeaderModule {}

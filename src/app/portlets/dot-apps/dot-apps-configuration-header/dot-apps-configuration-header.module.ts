import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsConfigurationHeaderComponent } from './dot-apps-configuration-header.component';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { MarkdownModule } from 'ngx-markdown';

@NgModule({
    imports: [
        CommonModule,
        DotAvatarModule,
        DotCopyButtonModule,
        DotPipesModule,
        MarkdownModule.forChild()
    ],
    declarations: [DotAppsConfigurationHeaderComponent],
    exports: [DotAppsConfigurationHeaderComponent]
})
export class DotAppsConfigurationHeaderModule {}

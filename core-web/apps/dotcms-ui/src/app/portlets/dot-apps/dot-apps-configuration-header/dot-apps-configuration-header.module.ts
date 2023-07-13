import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { DotAvatarDirective } from '@directives/dot-avatar/dot-avatar.directive';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationHeaderComponent } from './dot-apps-configuration-header.component';

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

import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { DotAvatarDirective, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotAppsConfigurationHeaderComponent } from './dot-apps-configuration-header.component';

@NgModule({
    imports: [
        CommonModule,
        AvatarModule,
        DotAvatarDirective,
        DotCopyLinkModule,
        DotSafeHtmlPipe,
        MarkdownModule.forChild(),
        DotMessagePipe
    ],
    declarations: [DotAppsConfigurationHeaderComponent],
    exports: [DotAppsConfigurationHeaderComponent]
})
export class DotAppsConfigurationHeaderModule {}

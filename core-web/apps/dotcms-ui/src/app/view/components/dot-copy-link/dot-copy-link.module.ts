import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { DotIconModule } from '@dotcms/ui';

import { DotCopyLinkComponent } from './dot-copy-link.component';

@NgModule({
    imports: [CommonModule, UiDotIconButtonModule, TooltipModule, DotIconModule, ButtonModule],
    declarations: [DotCopyLinkComponent],
    exports: [DotCopyLinkComponent],
    providers: [DotClipboardUtil]
})
export class DotCopyLinkModule {}

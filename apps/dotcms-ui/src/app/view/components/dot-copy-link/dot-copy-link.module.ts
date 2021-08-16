import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotCopyLinkComponent } from './dot-copy-link.component';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { TooltipModule } from 'primeng/tooltip';
import { DotIconModule } from '@dotcms/ui';

@NgModule({
    imports: [CommonModule, UiDotIconButtonModule, TooltipModule, DotIconModule],
    declarations: [DotCopyLinkComponent],
    exports: [DotCopyLinkComponent],
    providers: [DotClipboardUtil]
})
export class DotCopyLinkModule {}

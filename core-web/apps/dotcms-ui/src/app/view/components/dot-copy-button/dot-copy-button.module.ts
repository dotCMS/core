import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { TooltipModule } from 'primeng/tooltip';
import { DotCopyButtonComponent } from './dot-copy-button.component';

@NgModule({
    imports: [CommonModule, UiDotIconButtonModule, TooltipModule],
    declarations: [DotCopyButtonComponent],
    exports: [DotCopyButtonComponent],
    providers: [DotClipboardUtil]
})
export class DotCopyButtonModule {}

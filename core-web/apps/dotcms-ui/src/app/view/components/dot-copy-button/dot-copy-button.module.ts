import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';

import { DotCopyButtonComponent } from './dot-copy-button.component';

@NgModule({
    imports: [CommonModule, TooltipModule, ButtonModule],
    declarations: [DotCopyButtonComponent],
    exports: [DotCopyButtonComponent],
    providers: [DotClipboardUtil]
})
export class DotCopyButtonModule {}

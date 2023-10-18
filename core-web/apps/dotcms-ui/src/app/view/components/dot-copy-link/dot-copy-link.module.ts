import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotClipboardUtil, DotIconModule } from '@dotcms/ui';

import { DotCopyLinkComponent } from './dot-copy-link.component';

@NgModule({
    imports: [CommonModule, TooltipModule, DotIconModule, ButtonModule],
    declarations: [DotCopyLinkComponent],
    exports: [DotCopyLinkComponent],
    providers: [DotClipboardUtil]
})
export class DotCopyLinkModule {}

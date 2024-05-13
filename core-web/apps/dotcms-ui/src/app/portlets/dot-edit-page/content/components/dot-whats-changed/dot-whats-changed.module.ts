import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { IFrameModule } from '@components/_common/iframe';
import { DotDiffPipe, DotMessagePipe } from '@dotcms/ui';

import { DotWhatsChangedComponent } from './dot-whats-changed.component';

@NgModule({
    imports: [CommonModule, IFrameModule, DotDiffPipe, DotMessagePipe],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}

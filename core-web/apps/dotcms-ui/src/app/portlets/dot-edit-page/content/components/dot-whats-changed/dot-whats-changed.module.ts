import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { IFrameModule } from '@components/_common/iframe';
import { DotMessagePipe } from '@dotcms/ui';
import { DotDiffPipeModule } from '@pipes/dot-diff/dot-diff.pipe.module';

import { DotWhatsChangedComponent } from './dot-whats-changed.component';

@NgModule({
    imports: [CommonModule, IFrameModule, DotDiffPipeModule, DotMessagePipe],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}

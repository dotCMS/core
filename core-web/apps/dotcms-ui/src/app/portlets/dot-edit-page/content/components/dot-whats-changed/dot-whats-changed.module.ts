import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { IFrameModule } from '@components/_common/iframe';
import { DotDiffPipeModule } from '@pipes/dot-diff/dot-diff.pipe.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotWhatsChangedComponent } from './dot-whats-changed.component';

@NgModule({
    imports: [CommonModule, IFrameModule, DotDiffPipeModule, DotMessagePipeModule],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}

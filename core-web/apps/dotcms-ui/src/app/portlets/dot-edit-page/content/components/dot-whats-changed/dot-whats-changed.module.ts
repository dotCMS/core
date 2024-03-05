import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotWhatsChangedComponent } from './dot-whats-changed.component';

import { IFrameModule } from '../../../../../view/components/_common/iframe/iframe.module';
import { DotDiffPipeModule } from '../../../../../view/pipes/dot-diff/dot-diff.pipe.module';

@NgModule({
    imports: [CommonModule, IFrameModule, DotDiffPipeModule, DotMessagePipe],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}

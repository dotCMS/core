import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotDiffPipe, DotMessagePipe } from '@dotcms/ui';

import { DotWhatsChangedComponent } from './dot-whats-changed.component';

import { IFrameModule } from '../../../../../view/components/_common/iframe/iframe.module';

@NgModule({
    imports: [CommonModule, IFrameModule, DotDiffPipe, DotMessagePipe],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}

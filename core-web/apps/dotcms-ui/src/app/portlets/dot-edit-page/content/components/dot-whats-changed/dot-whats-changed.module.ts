import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotDiffPipe, DotMessagePipe } from '@dotcms/ui';

import { DotWhatsChangedComponent } from './dot-whats-changed.component';

import { IframeComponent } from '../../../../../view/components/_common/iframe/iframe-component/iframe.component';

@NgModule({
    imports: [CommonModule, IframeComponent, DotDiffPipe, DotMessagePipe],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}

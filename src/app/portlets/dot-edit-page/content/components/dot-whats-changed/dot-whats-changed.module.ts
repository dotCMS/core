import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotWhatsChangedComponent } from './dot-whats-changed.component';
import { IFrameModule } from '@components/_common/iframe';

@NgModule({
    imports: [CommonModule, IFrameModule],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}

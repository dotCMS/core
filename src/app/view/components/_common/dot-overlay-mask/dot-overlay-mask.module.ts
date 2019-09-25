import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotOverlayMaskComponent } from '@components/_common/dot-overlay-mask/dot-overlay-mask.component';

@NgModule({
    declarations: [DotOverlayMaskComponent],
    exports: [DotOverlayMaskComponent],
    imports: [CommonModule]
})
export class DotOverlayMaskModule {}

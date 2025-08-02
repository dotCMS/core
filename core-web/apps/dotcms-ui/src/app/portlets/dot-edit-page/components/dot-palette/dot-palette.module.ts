import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotPaletteContentTypeModule } from './dot-palette-content-type/dot-palette-content-type.module';
import { DotPaletteContentletsModule } from './dot-palette-contentlets/dot-palette-contentlets.module';
import { DotPaletteComponent } from './dot-palette.component';

@NgModule({
    imports: [CommonModule, DotPaletteContentTypeModule, DotPaletteContentletsModule],
    declarations: [DotPaletteComponent],
    exports: [DotPaletteComponent]
})
export class DotPaletteModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotPaletteContentTypeComponent } from '@dotcms/app/portlets/dot-edit-page/components/dot-palette/dot-palette-content-type/dot-palette-content-type.component';
import { DotIconModule, DotSpinnerModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';

@NgModule({
    imports: [
        CommonModule,
        DotPipesModule,
        DotIconModule,
        DotSpinnerModule,
        DotPaletteInputFilterModule
    ],
    declarations: [DotPaletteContentTypeComponent],
    exports: [DotPaletteContentTypeComponent]
})
export class DotPaletteContentTypeModule {}

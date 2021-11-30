import { NgModule } from '@angular/core';
import { DotPaletteContentTypeComponent } from '@dotcms/app/portlets/dot-edit-page/components/dot-palette/dot-palette-content-type/dot-palette-content-type.component';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';

@NgModule({
    imports: [
        CommonModule,
        DotPipesModule,
        DotIconModule,
        DotPaletteInputFilterModule,
    ],
    declarations: [DotPaletteContentTypeComponent],
    exports: [DotPaletteContentTypeComponent]
})
export class DotPaletteContentTypeModule {}

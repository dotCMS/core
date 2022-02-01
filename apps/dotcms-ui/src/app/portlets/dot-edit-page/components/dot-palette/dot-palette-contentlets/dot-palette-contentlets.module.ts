import { NgModule } from '@angular/core';
import { DotPaletteContentletsComponent } from '@dotcms/app/portlets/dot-edit-page/components/dot-palette/dot-palette-contentlets/dot-palette-contentlets.component';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { PaginatorModule } from 'primeng/paginator';
import { DotSpinnerModule } from '@dotcms/ui';
import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';

@NgModule({
    imports: [
        CommonModule,
        DotPipesModule,
        DotPaletteInputFilterModule,
        PaginatorModule,
        DotSpinnerModule
    ],
    declarations: [DotPaletteContentletsComponent],
    exports: [DotPaletteContentletsComponent]
})
export class DotPaletteContentletsModule {}

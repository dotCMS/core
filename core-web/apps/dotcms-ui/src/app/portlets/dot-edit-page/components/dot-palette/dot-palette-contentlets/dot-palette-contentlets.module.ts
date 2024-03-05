import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { PaginatorModule } from 'primeng/paginator';

import { DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

import { DotPaletteContentletsComponent } from './dot-palette-contentlets.component';

import { DotPipesModule } from '../../../../../view/pipes/dot-pipes.module';
import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';

@NgModule({
    imports: [
        CommonModule,
        DotPipesModule,
        DotPaletteInputFilterModule,
        PaginatorModule,
        DotSpinnerModule,
        DotMessagePipe
    ],
    declarations: [DotPaletteContentletsComponent],
    exports: [DotPaletteContentletsComponent]
})
export class DotPaletteContentletsModule {}

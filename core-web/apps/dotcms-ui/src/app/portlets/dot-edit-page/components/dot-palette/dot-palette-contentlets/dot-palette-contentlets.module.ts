import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { PaginatorModule } from 'primeng/paginator';

import { DotMessagePipe, DotSafeHtmlPipe, DotSpinnerModule } from '@dotcms/ui';

import { DotPaletteContentletsComponent } from './dot-palette-contentlets.component';

import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';

@NgModule({
    imports: [
        CommonModule,
        DotSafeHtmlPipe,
        DotPaletteInputFilterModule,
        PaginatorModule,
        DotSpinnerModule,
        DotMessagePipe
    ],
    declarations: [DotPaletteContentletsComponent],
    exports: [DotPaletteContentletsComponent]
})
export class DotPaletteContentletsModule {}

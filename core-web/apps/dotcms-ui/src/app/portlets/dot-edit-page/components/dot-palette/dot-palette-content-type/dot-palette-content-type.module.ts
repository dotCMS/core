import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotIconModule, DotMessagePipe, DotSafeHtmlPipe, DotSpinnerModule } from '@dotcms/ui';

import { DotPaletteContentTypeComponent } from './dot-palette-content-type.component';

import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';

@NgModule({
    imports: [
        CommonModule,
        DotMessagePipe,
        DotSafeHtmlPipe,
        DotIconModule,
        DotSpinnerModule,
        DotPaletteInputFilterModule
    ],
    declarations: [DotPaletteContentTypeComponent],
    exports: [DotPaletteContentTypeComponent]
})
export class DotPaletteContentTypeModule {}

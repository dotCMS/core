import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPaletteInputFilterComponent } from './dot-palette-input-filter.component';

@NgModule({
    imports: [
        CommonModule,
        DotSafeHtmlPipe,
        DotIconComponent,
        FormsModule,
        InputTextModule,
        DotMessagePipe
    ],
    declarations: [DotPaletteInputFilterComponent],
    exports: [DotPaletteInputFilterComponent]
})
export class DotPaletteInputFilterModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { DotPaletteInputFilterComponent } from './dot-palette-input-filter.component';

import { DotPipesModule } from '../../../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        DotPipesModule,
        DotIconModule,
        FormsModule,
        InputTextModule,
        DotMessagePipe
    ],
    declarations: [DotPaletteInputFilterComponent],
    exports: [DotPaletteInputFilterComponent]
})
export class DotPaletteInputFilterModule {}

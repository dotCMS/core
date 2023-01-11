import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPaletteInputFilterComponent } from './dot-palette-input-filter.component';

@NgModule({
    imports: [CommonModule, DotPipesModule, DotIconModule, FormsModule, InputTextModule],
    declarations: [DotPaletteInputFilterComponent],
    exports: [DotPaletteInputFilterComponent]
})
export class DotPaletteInputFilterModule {}

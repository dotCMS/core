import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPaletteInputFilterComponent } from './dot-palette-input-filter.component';
import { FormsModule } from '@angular/forms';

@NgModule({
    imports: [CommonModule, DotPipesModule, DotIconModule, FormsModule],
    declarations: [DotPaletteInputFilterComponent],
    exports: [DotPaletteInputFilterComponent]
})
export class DotPaletteInputFilterModule {}

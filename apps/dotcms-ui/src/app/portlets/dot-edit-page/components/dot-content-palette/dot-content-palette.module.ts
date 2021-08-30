import { NgModule } from '@angular/core';
import { DotContentPaletteComponent } from '@portlets/dot-edit-page/components/dot-content-palette/dot-content-palette.component';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { DotFilterPipeModule } from '@pipes/dot-filter/dot-filter-pipe.module';
import { FormsModule } from '@angular/forms';

@NgModule({
    imports: [CommonModule, DotPipesModule, DotIconModule, DotFilterPipeModule, FormsModule],
    declarations: [DotContentPaletteComponent],
    exports: [DotContentPaletteComponent]
})
export class DotContentPaletteModule {}

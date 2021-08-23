import { NgModule } from '@angular/core';
import { DotContentPaletteComponent } from '@portlets/dot-edit-page/components/dot-content-palette/dot-content-palette.component';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';

@NgModule({
    imports: [CommonModule, DotPipesModule, DotIconModule],
    declarations: [DotContentPaletteComponent],
    exports: [DotContentPaletteComponent]
})
export class DotContentPaletteModule {}

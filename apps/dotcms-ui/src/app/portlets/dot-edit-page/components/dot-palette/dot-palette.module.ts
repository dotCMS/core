import { NgModule } from '@angular/core';
import { DotPaletteComponent } from '@dotcms/app/portlets/dot-edit-page/components/dot-palette/dot-palette.component';
import { DotPaletteContentTypeModule } from './dot-palette-content-type/dot-palette-content-type.module';
import { DotPaletteContentletsModule } from './dot-palette-contentlets/dot-palette-contentlets.module';

@NgModule({
    imports: [
        DotPaletteContentTypeModule,
        DotPaletteContentletsModule
    ],
    declarations: [DotPaletteComponent],
    exports: [DotPaletteComponent]
})
export class DotPaletteModule {}

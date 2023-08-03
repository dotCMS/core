import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPreviewLinkComponent } from './dot-preview-link.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, DotPipesModule],
    declarations: [DotPreviewLinkComponent],
    exports: [DotPreviewLinkComponent]
})
export class DotPreviewLinkModule {}

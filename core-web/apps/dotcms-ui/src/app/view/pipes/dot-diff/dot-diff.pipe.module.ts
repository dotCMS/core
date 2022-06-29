import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotDiffPipe } from '@dotcms/app/view/pipes';

@NgModule({
    imports: [CommonModule],
    declarations: [DotDiffPipe],
    exports: [DotDiffPipe]
})
export class DotDiffPipeModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotDiffPipe } from '@dotcms/app/view/pipes';

@NgModule({
    imports: [CommonModule],
    declarations: [DotDiffPipe],
    exports: [DotDiffPipe]
})
export class DotDiffPipeModule {}

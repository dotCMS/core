import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotFilterPipe } from './dot-filter.pipe';

@NgModule({
    imports: [CommonModule],
    declarations: [DotFilterPipe],
    exports: [DotFilterPipe]
})
export class DotFilterPipeModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotFilterPipe } from '@pipes/dot-filter/dot-filter.pipe';

@NgModule({
    imports: [CommonModule],
    declarations: [DotFilterPipe],
    exports: [DotFilterPipe]
})
export class DotFilterPipeModule {}

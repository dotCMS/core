import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotIconComponent } from './dot-icon.component';

@NgModule({
    declarations: [DotIconComponent],
    exports: [DotIconComponent],
    imports: [CommonModule]
})
export class DotIconModule {}

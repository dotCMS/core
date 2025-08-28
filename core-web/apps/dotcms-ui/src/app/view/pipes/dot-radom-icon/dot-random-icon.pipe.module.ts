import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotRandomIconPipe } from './dot-random-icon.pipe';

@NgModule({
    imports: [CommonModule],
    declarations: [DotRandomIconPipe],
    exports: [DotRandomIconPipe]
})
export class DotRandomIconPipeModule {}

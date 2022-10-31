import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotRandomIconPipe } from '@pipes/dot-radom-icon/dot-random-icon.pipe';

@NgModule({
    imports: [CommonModule],
    declarations: [DotRandomIconPipe],
    exports: [DotRandomIconPipe]
})
export class DotRandomIconPipeModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotNavIconComponent } from './dot-nav-icon.component';
import { DotIconModule } from '../../../_common/dot-icon/dot-icon.module';

@NgModule({
    imports: [CommonModule, DotIconModule],
    declarations: [DotNavIconComponent],
    exports: [DotNavIconComponent]
})
export class DotNavIconModule {}

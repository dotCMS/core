import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotNavIconComponent } from './dot-nav-icon.component';
import { DotIconModule } from '@dotcms/ui';

@NgModule({
    imports: [CommonModule, DotIconModule],
    declarations: [DotNavIconComponent],
    exports: [DotNavIconComponent]
})
export class DotNavIconModule {}

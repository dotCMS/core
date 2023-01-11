import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotIconModule } from '@dotcms/ui';

import { DotNavIconComponent } from './dot-nav-icon.component';

@NgModule({
    imports: [CommonModule, DotIconModule],
    declarations: [DotNavIconComponent],
    exports: [DotNavIconComponent]
})
export class DotNavIconModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

import { DotIconModule } from '@dotcms/ui';

import { DotPagesCardEmptyComponent } from './dot-pages-card-empty.component';

@NgModule({
    imports: [CommonModule, SkeletonModule, DotIconModule],
    declarations: [DotPagesCardEmptyComponent],
    exports: [DotPagesCardEmptyComponent]
})
export class DotPagesCardEmptyModule {}

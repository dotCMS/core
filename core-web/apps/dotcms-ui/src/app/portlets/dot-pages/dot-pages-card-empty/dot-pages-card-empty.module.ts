import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { DotPagesCardEmptyComponent } from './dot-pages-card-empty.component';
import { DotIconModule } from '@dotcms/ui';

@NgModule({
    imports: [CommonModule, SkeletonModule, DotIconModule],
    declarations: [DotPagesCardEmptyComponent],
    exports: [DotPagesCardEmptyComponent]
})
export class DotPagesCardEmptyModule {}

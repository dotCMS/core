import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotIconModule } from '@dotcms/ui';
import { SkeletonModule } from 'primeng/skeleton';
import { DotPagesCardEmptyComponent } from './dot-pages-card-empty.component';

@NgModule({
    imports: [CommonModule, SkeletonModule, DotIconModule],
    declarations: [DotPagesCardEmptyComponent],
    exports: [DotPagesCardEmptyComponent]
})
export class DotPagesCardEmptyModule {}

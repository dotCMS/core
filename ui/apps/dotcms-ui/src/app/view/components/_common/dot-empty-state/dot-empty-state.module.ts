import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEmptyStateComponent } from './dot-empty-state.component';
import { ButtonModule } from 'primeng/button';

@NgModule({
    declarations: [DotEmptyStateComponent],
    imports: [CommonModule, ButtonModule],
    exports: [DotEmptyStateComponent]
})
export class DotEmptyStateModule {}

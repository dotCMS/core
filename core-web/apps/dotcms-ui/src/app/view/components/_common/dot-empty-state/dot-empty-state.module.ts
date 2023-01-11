import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotEmptyStateComponent } from './dot-empty-state.component';

@NgModule({
    declarations: [DotEmptyStateComponent],
    imports: [CommonModule, ButtonModule],
    exports: [DotEmptyStateComponent]
})
export class DotEmptyStateModule {}

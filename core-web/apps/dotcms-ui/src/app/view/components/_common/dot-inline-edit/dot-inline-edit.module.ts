import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SharedModule } from 'primeng/api';
import { InplaceModule } from 'primeng/inplace';

import { DotInlineEditComponent } from './dot-inline-edit.component';

@NgModule({
    imports: [CommonModule, InplaceModule, SharedModule],
    declarations: [DotInlineEditComponent],
    exports: [DotInlineEditComponent],
    providers: []
})
export class DotInlineEditModule {}

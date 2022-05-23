import { NgModule } from '@angular/core';
import { DotInlineEditComponent } from './dot-inline-edit.component';
import { InplaceModule } from 'primeng/inplace';
import { SharedModule } from 'primeng/api';
import { CommonModule } from '@angular/common';

@NgModule({
    imports: [CommonModule, InplaceModule, SharedModule],
    declarations: [DotInlineEditComponent],
    exports: [DotInlineEditComponent],
    providers: []
})
export class DotInlineEditModule {}

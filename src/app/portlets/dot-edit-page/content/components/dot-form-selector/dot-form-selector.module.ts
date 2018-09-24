import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotFormSelectorComponent } from './dot-form-selector.component';
import { DataTableModule, ButtonModule } from 'primeng/primeng';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@NgModule({
    imports: [CommonModule, DataTableModule, DotDialogModule, ButtonModule],
    declarations: [DotFormSelectorComponent],
    exports: [DotFormSelectorComponent]
})
export class DotFormSelectorModule {}

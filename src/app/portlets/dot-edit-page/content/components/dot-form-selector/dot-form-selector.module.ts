import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotFormSelectorComponent } from './dot-form-selector.component';
import { DataTableModule, ButtonModule } from 'primeng/primeng';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, DataTableModule, DotDialogModule, ButtonModule, DotPipesModule],
    declarations: [DotFormSelectorComponent],
    exports: [DotFormSelectorComponent]
})
export class DotFormSelectorModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotFormSelectorComponent } from './dot-form-selector.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, TableModule, DotDialogModule, ButtonModule, DotPipesModule],
    declarations: [DotFormSelectorComponent],
    exports: [DotFormSelectorComponent]
})
export class DotFormSelectorModule {}

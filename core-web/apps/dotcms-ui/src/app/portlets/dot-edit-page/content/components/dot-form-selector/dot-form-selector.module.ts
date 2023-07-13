import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotFormSelectorComponent } from './dot-form-selector.component';

@NgModule({
    imports: [
        CommonModule,
        TableModule,
        DotDialogModule,
        ButtonModule,
        DotPipesModule,
        DotMessagePipe
    ],
    declarations: [DotFormSelectorComponent],
    exports: [DotFormSelectorComponent]
})
export class DotFormSelectorModule {}

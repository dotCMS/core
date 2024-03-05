import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '@dotcms/ui';

import { DotFormSelectorComponent } from './dot-form-selector.component';

import { DotDialogModule } from '../../../../../view/components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '../../../../../view/pipes/dot-pipes.module';

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

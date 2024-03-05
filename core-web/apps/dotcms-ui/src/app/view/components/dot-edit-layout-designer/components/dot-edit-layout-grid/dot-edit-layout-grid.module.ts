import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { NgGridModule } from '@dotcms/dot-layout-grid';
import { DotAutofocusDirective, DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';

import { DotPipesModule } from '../../../../pipes/dot-pipes.module';
import { DotActionButtonModule } from '../../../_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorLayoutModule } from '../../../dot-container-selector-layout/dot-container-selector-layout.module';
import { DotDialogModule } from '../../../dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotEditLayoutGridComponent],
    imports: [
        CommonModule,
        NgGridModule,
        DotActionButtonModule,
        DotContainerSelectorLayoutModule,
        ButtonModule,
        DotDialogModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule,
        TooltipModule,
        DotAutofocusDirective,
        DotPipesModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    exports: [DotEditLayoutGridComponent],
    providers: []
})
export class DotEditLayoutGridModule {}

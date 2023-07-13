import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorLayoutModule } from '@components/dot-container-selector-layout/dot-container-selector-layout.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { NgGridModule } from '@dotcms/dot-layout-grid';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';

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
        DotAutofocusModule,
        DotPipesModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    exports: [DotEditLayoutGridComponent],
    providers: []
})
export class DotEditLayoutGridModule {}

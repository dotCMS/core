import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { ButtonModule, InputTextModule } from 'primeng/primeng';
import { NgGridModule } from 'dot-layout-grid';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    declarations: [DotEditLayoutGridComponent],
    imports: [
        CommonModule,
        NgGridModule,
        DotActionButtonModule,
        DotContainerSelectorModule,
        ButtonModule,
        DotDialogModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule,
        DotIconButtonTooltipModule,
        DotAutofocusModule,
        MdInputTextModule,
        DotPipesModule
    ],
    exports: [DotEditLayoutGridComponent],
    providers: []
})
export class DotEditLayoutGridModule {}

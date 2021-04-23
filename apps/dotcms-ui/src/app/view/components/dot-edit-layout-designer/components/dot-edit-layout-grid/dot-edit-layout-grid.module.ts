import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorLayoutModule } from '@components/dot-container-selector-layout/dot-container-selector-layout.module';
import { NgGridModule } from '@dotcms/dot-layout-grid';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

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
        UiDotIconButtonTooltipModule,
        DotAutofocusModule,
        DotPipesModule
    ],
    exports: [DotEditLayoutGridComponent],
    providers: []
})
export class DotEditLayoutGridModule {}

import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { ButtonModule, InputTextModule } from 'primeng/primeng';
import { NgGridModule } from 'dot-layout-grid';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

@NgModule({
    declarations: [DotEditLayoutGridComponent],
    imports: [
        CommonModule,
        NgGridModule,
        DotActionButtonModule,
        DotContainerSelectorModule,
        ButtonModule,
        DotDialogModule,
        DotIconButtonModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule
    ],
    exports: [DotEditLayoutGridComponent],
    providers: []
})
export class DotEditLayoutGridModule {}

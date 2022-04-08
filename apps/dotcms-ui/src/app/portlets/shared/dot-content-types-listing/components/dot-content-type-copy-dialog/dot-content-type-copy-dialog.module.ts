import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';

import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMdIconSelectorModule } from '@components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

import { InputTextModule } from 'primeng/inputtext';
import { DotContentTypeCopyDialogComponent } from './dot-content-type-copy-dialog.component';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotPipesModule,
        DotFieldValidationMessageModule,
        DotDialogModule,
        DotMdIconSelectorModule,
        SiteSelectorFieldModule,
        DotAutofocusModule
    ],
    declarations: [DotContentTypeCopyDialogComponent],
    exports: [DotContentTypeCopyDialogComponent]
})
export class DotContentTypeCopyDialogModule {}

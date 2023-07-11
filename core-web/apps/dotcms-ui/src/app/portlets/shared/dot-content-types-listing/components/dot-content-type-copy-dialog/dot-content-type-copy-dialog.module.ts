import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMdIconSelectorModule } from '@components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotContentTypeCopyDialogComponent } from './dot-content-type-copy-dialog.component';

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
        DotAutofocusModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotContentTypeCopyDialogComponent],
    exports: [DotContentTypeCopyDialogComponent]
})
export class DotContentTypeCopyDialogModule {}

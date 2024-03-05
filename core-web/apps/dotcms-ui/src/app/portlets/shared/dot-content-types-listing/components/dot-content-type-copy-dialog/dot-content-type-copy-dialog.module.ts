import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotContentTypeCopyDialogComponent } from './dot-content-type-copy-dialog.component';

import { DotMdIconSelectorModule } from '../../../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { SiteSelectorFieldModule } from '../../../../../view/components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotBaseTypeSelectorModule } from '../../../../../view/components/dot-base-type-selector/dot-base-type-selector.module';
import { DotDialogModule } from '../../../../../view/components/dot-dialog/dot-dialog.module';
import { DotListingDataTableModule } from '../../../../../view/components/dot-listing-data-table/dot-listing-data-table.module';
import { DotPipesModule } from '../../../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotPipesModule,
        DotFieldValidationMessageComponent,
        DotDialogModule,
        DotMdIconSelectorModule,
        SiteSelectorFieldModule,
        DotAutofocusDirective,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotContentTypeCopyDialogComponent],
    exports: [DotContentTypeCopyDialogComponent]
})
export class DotContentTypeCopyDialogModule {}

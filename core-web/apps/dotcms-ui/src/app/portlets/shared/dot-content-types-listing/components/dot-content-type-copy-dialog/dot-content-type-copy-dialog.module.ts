import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import {
    DotAutofocusDirective,
    DotDialogComponent,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotContentTypeCopyDialogComponent } from './dot-content-type-copy-dialog.component';

import { DotMdIconSelectorComponent } from '../../../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.component';
import { DotSiteSelectorFieldComponent } from '../../../../../view/components/_common/dot-site-selector-field/dot-site-selector-field.component';
import { DotBaseTypeSelectorComponent } from '../../../../../view/components/dot-base-type-selector/dot-base-type-selector.component';
import { DotListingDataTableComponent } from '../../../../../view/components/dot-listing-data-table/dot-listing-data-table.component';

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        DotListingDataTableComponent,
        DotBaseTypeSelectorComponent,
        DotSafeHtmlPipe,
        DotFieldValidationMessageComponent,
        DotDialogComponent,
        DotMdIconSelectorComponent,
        DotSiteSelectorFieldComponent,
        DotAutofocusDirective,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    declarations: [DotContentTypeCopyDialogComponent],
    exports: [DotContentTypeCopyDialogComponent]
})
export class DotContentTypeCopyDialogModule {}

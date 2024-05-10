import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotMdIconSelectorModule } from '@components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotBaseTypeSelectorModule } from '@components/dot-base-type-selector';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import {
    DotAutofocusDirective,
    DotDialogModule,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotContentTypeCopyDialogComponent } from './dot-content-type-copy-dialog.component';

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        DotListingDataTableModule,
        DotBaseTypeSelectorModule,
        DotSafeHtmlPipe,
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

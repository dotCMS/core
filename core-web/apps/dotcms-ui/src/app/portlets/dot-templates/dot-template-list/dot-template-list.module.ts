import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SharedModule } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotBulkInformationModule } from '@components/_common/dot-bulk-information/dot-bulk-information.module';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotTemplatesService } from '@dotcms/app/api/services/dot-templates/dot-templates.service';
import { DotSiteBrowserService } from '@dotcms/data-access';
import {
    DotActionMenuButtonComponent,
    DotAddToBundleComponent,
    DotBinaryOptionSelectorComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';
import { DotTemplateListResolver } from '@portlets/dot-templates/dot-template-list/dot-template-list-resolver.service';

import { DotTemplateListComponent } from './dot-template-list.component';

@NgModule({
    declarations: [DotTemplateListComponent],
    imports: [
        CommonModule,
        DotListingDataTableModule,
        DotMessagePipe,
        DotRelativeDatePipe,
        SharedModule,
        CheckboxModule,
        MenuModule,
        ButtonModule,
        DotActionButtonModule,
        DotActionMenuButtonComponent,
        DotAddToBundleComponent,
        DynamicDialogModule,
        DotBulkInformationModule,
        DotBinaryOptionSelectorComponent,
        DotEmptyStateModule,
        AutoFocusModule
    ],
    providers: [DotTemplateListResolver, DotTemplatesService, DialogService, DotSiteBrowserService]
})
export class DotTemplateListModule {}

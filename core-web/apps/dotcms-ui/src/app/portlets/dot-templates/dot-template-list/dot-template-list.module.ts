import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SharedModule } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotBulkInformationModule } from '@components/_common/dot-bulk-information/dot-bulk-information.module';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { DotTemplatesService } from '@dotcms/app/api/services/dot-templates/dot-templates.service';
import { DotRelativeDatePipe } from '@dotcms/app/view/pipes/dot-relative-date/dot-relative-date.pipe';
import { DotSiteBrowserService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { DotTemplateListResolver } from '@portlets/dot-templates/dot-template-list/dot-template-list-resolver.service';
import { DotBinaryOptionSelectorComponent } from '@portlets/shared/dot-binary-option-selector/dot-binary-option-selector.component';

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
        DotActionMenuButtonModule,
        DotAddToBundleModule,
        DynamicDialogModule,
        DotBulkInformationModule,
        DotBinaryOptionSelectorComponent,
        DotEmptyStateModule,
        AutoFocusModule
    ],
    providers: [DotTemplateListResolver, DotTemplatesService, DialogService, DotSiteBrowserService]
})
export class DotTemplateListModule {}

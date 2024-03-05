import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SharedModule } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';

import { DotSiteBrowserService } from '@dotcms/data-access';
import { DotBinaryOptionSelectorComponent, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotTemplateListResolver } from './dot-template-list-resolver.service';
import { DotTemplateListComponent } from './dot-template-list.component';

import { DotTemplatesService } from '../../../api/services/dot-templates/dot-templates.service';
import { DotActionButtonModule } from '../../../view/components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '../../../view/components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '../../../view/components/_common/dot-add-to-bundle/dot-add-to-bundle.module';
import { DotBulkInformationModule } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.module';
import { DotEmptyStateModule } from '../../../view/components/_common/dot-empty-state/dot-empty-state.module';
import { DotListingDataTableModule } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.module';

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

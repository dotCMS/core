import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateListComponent } from './dot-template-list.component';
import { DotTemplateListResolver } from '@portlets/dot-templates/dot-template-list/dot-template-list-resolver.service';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { SharedModule } from 'primeng/api';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotBulkInformationModule } from '@components/_common/dot-bulk-information/dot-bulk-information.module';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotTemplateSelectorModule } from './components/dot-template-selector/dot-template-selector.module';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';

@NgModule({
    declarations: [DotTemplateListComponent],
    imports: [
        CommonModule,
        DotListingDataTableModule,
        DotMessagePipeModule,
        SharedModule,
        CheckboxModule,
        MenuModule,
        ButtonModule,
        DotActionButtonModule,
        DotActionMenuButtonModule,
        DotAddToBundleModule,
        DynamicDialogModule,
        DotBulkInformationModule,
        DotTemplateSelectorModule,
        DotEmptyStateModule
    ],
    providers: [DotTemplateListResolver, DotTemplatesService, DialogService]
})
export class DotTemplateListModule {}

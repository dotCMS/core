import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SharedModule } from 'primeng/api';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';

import { DotTemplatesService } from '../dot-templates.service';
import { DotTemplateListComponent } from './dot-template-list.component';

import { DotListingDataTableModule } from '../../../../../apps/dotcms-ui/src/app/view/components/dot-listing-data-table';
import { DotMessagePipeModule } from '../../../../../apps/dotcms-ui/src/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotAddToBundleModule } from '../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-add-to-bundle';
import { DotSiteBrowserService } from '../../../../../apps/dotcms-ui/src/app/api/services/dot-site-browser/dot-site-browser.service';
import { DotTemplateListResolver } from './dot-template-list-resolver.service';
import { DotEmptyStateModule } from '../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-empty-state/dot-empty-state.module';
import { DotActionButtonModule } from '../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotBulkInformationModule } from '../../../../../apps/dotcms-ui/src/app/view/components/_common/dot-bulk-information/dot-bulk-information.module';
import { DotTemplateSelectorModule } from './components/dot-template-selector/dot-template-selector.module';

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
    providers: [DotTemplateListResolver, DotTemplatesService, DialogService, DotSiteBrowserService]
})
export class DotTemplateListModule {}

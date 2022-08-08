import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotTemplatesRoutingModule } from './dot-templates-routing.module';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotTemplateListModule } from '../../../../../../libs/dotcms-ui/templates/feature-list/src/lib/dot-template-list.module';
import { DotTemplateCreateEditResolver } from '../../../../../../libs/dotcms-ui/templates/feature-create-edit/src/lib/dot-template-create-edit.resolver';
import { LayoutEditorCanDeactivateGuardService } from '@services/guards/layout-editor-can-deactivate-guard.service';

@NgModule({
    declarations: [],
    imports: [CommonModule, DotTemplatesRoutingModule, DotTemplateListModule],
    providers: [
        DotTemplatesService,
        DotTemplateCreateEditResolver,
        LayoutEditorCanDeactivateGuardService
    ]
})
export class DotTemplatesModule {}

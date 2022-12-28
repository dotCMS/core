import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotTemplatesRoutingModule } from './dot-templates-routing.module';
import { DotTemplateListModule } from '@portlets/dot-templates/dot-template-list/dot-template-list.module';
import { DotTemplateCreateEditResolver } from './dot-template-create-edit/resolvers/dot-template-create-edit.resolver';
import { LayoutEditorCanDeactivateGuardService } from '@dotcms/app/api/services/guards/layout-editor-can-deactivate-guard.service';
import { DotTemplatesService } from '@dotcms/app/api/services/dot-templates/dot-templates.service';

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

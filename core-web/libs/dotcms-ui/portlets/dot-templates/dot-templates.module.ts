import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplatesRoutingModule } from './dot-templates-routing.module';
import { DotTemplatesService } from './dot-templates.service';
import { DotTemplateListModule } from './dot-template-list/dot-template-list.module';
import { DotTemplateCreateEditResolver } from './dot-template-create-edit/resolvers/dot-template-create-edit.resolver';
import { LayoutEditorCanDeactivateGuardService } from '@dotcms-ui/shared';

@NgModule({
    imports: [CommonModule, DotTemplatesRoutingModule, DotTemplateListModule],
    providers: [
        DotTemplatesService,
        DotTemplateCreateEditResolver,
        LayoutEditorCanDeactivateGuardService
    ]
})
export class DotTemplatesModule {}

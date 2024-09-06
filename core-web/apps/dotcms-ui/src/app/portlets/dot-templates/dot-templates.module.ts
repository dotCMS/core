import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotTemplatesService } from '@dotcms/app/api/services/dot-templates/dot-templates.service';
import { CanDeactivateGuardService } from '@dotcms/data-access';
import { DotTemplateListModule } from '@portlets/dot-templates/dot-template-list/dot-template-list.module';

import { DotTemplateCreateEditResolver } from './dot-template-create-edit/resolvers/dot-template-create-edit.resolver';
import { DotTemplatesRoutingModule } from './dot-templates-routing.module';

@NgModule({
    declarations: [],
    imports: [CommonModule, DotTemplatesRoutingModule, DotTemplateListModule],
    providers: [DotTemplatesService, DotTemplateCreateEditResolver, CanDeactivateGuardService]
})
export class DotTemplatesModule {}

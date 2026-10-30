import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CanDeactivateGuardService } from '@dotcms/data-access';

import { DotTemplateCreateEditResolver } from './dot-template-create-edit/resolvers/dot-template-create-edit.resolver';
import { DotTemplateListModule } from './dot-template-list/dot-template-list.module';
import { DotTemplatesRoutingModule } from './dot-templates-routing.module';

import { DotTemplatesService } from '../../api/services/dot-templates/dot-templates.service';

@NgModule({
    declarations: [],
    imports: [CommonModule, DotTemplatesRoutingModule, DotTemplateListModule],
    providers: [DotTemplatesService, DotTemplateCreateEditResolver, CanDeactivateGuardService]
})
export class DotTemplatesModule {}

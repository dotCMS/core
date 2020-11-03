import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotTemplatesRoutingModule } from './dot-templates-routing.module';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotTemplateListModule } from '@portlets/dot-templates/dot-template-list/dot-template-list.module';

@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        DotTemplatesRoutingModule,
        DotTemplatesRoutingModule,
        DotTemplateListModule
    ],
    providers: [DotTemplatesService]
})
export class DotTemplatesModule {}

import { NgModule } from '@angular/core';
import { DotTemplatesRoutingModule } from '@portlets/dot-templates/dot-templates-routing.module';
import { DotTemplateListModule } from '@portlets/dot-templates/dot-template-list/dot-template-list.module';

@NgModule({ imports: [DotTemplatesRoutingModule, DotTemplateListModule] })
export class DotTemplatesModule {}

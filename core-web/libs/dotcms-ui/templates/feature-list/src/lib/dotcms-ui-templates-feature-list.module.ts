import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateListModule } from './dot-template-list.module';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';

@NgModule({
    imports: [CommonModule, DotTemplateListModule],
    providers: [DotTemplatesService]
})
export class DotcmsUiTemplatesFeatureListModule {}

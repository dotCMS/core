import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CanDeactivateGuardService } from '@dotcms/data-access';

import { DotTemplateNewRoutingModule } from './dot-template-new-routing.module';
import { DotTemplateNewComponent } from './dot-template-new.component';
import { DotTemplateGuard } from './guards/dot-template.guard';

@NgModule({
    declarations: [DotTemplateNewComponent],
    imports: [CommonModule, DotTemplateNewRoutingModule],
    providers: [DotTemplateGuard, CanDeactivateGuardService]
})
export class DotTemplateNewModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateNewComponent } from './dot-template-new.component';
import { DotTemplateNewRoutingModule } from './dot-template-new-routing.module';
import { DotTemplateGuard } from './guards/dot-template.guard';

@NgModule({
    declarations: [DotTemplateNewComponent],
    imports: [CommonModule, DotTemplateNewRoutingModule],
    providers: [DotTemplateGuard]
})
export class DotTemplateNewModule {}

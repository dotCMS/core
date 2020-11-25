import { NgModule } from '@angular/core';
import { DotTemplateComponent } from './dot-template-advanced.component';
import { InputTextModule } from 'primeng/inputtext';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { ButtonModule } from 'primeng/button';
import { ReactiveFormsModule } from '@angular/forms';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotTemplateRoutingModule } from './dot-template-advanced-routing.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotPortletToolbarModule } from '@components/dot-portlet-base/components/dot-portlet-toolbar/dot-portlet-toolbar.module';
import { CommonModule } from '@angular/common';

@NgModule({
    declarations: [DotTemplateComponent],
    imports: [
        InputTextModule,
        ButtonModule,
        DotTextareaContentModule,
        ReactiveFormsModule,
        DotFieldValidationMessageModule,
        DotContainerSelectorModule,
        DotTemplateRoutingModule,
        DotPortletBaseModule,
        DotPortletToolbarModule,
        CommonModule
    ],
    providers: [DotRouterService]
})
export class DotTemplateModule {}

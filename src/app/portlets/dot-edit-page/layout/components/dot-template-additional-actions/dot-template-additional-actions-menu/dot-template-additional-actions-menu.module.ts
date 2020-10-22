import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateAdditionalActionsMenuComponent } from './dot-template-additional-actions-menu.component';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';

@NgModule({
    declarations: [DotTemplateAdditionalActionsMenuComponent],
    imports: [MenuModule, ButtonModule, CommonModule],
    exports: [DotTemplateAdditionalActionsMenuComponent]
})
export class DotTemplateAdditionalActionsMenuModule {}

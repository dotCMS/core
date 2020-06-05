import { NgModule } from '@angular/core';
import { MenuModule, ButtonModule } from 'primeng/primeng';
import { CommonModule } from '@angular/common';
import { DotTemplateAdditionalActionsMenuComponent } from './dot-template-additional-actions-menu.component';

@NgModule({
    declarations: [DotTemplateAdditionalActionsMenuComponent],
    imports: [MenuModule, ButtonModule, CommonModule],
    exports: [DotTemplateAdditionalActionsMenuComponent],
})
export class DotTemplateAdditionalActionsMenuModule {}

import { NgModule } from '@angular/core';
import { MenuModule, ButtonModule } from 'primeng/primeng';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { CommonModule } from '@angular/common';
import { DotTemplateAdditionalActionsMenuComponent } from './dot-template-additional-actions-menu.component';


@NgModule({
    declarations: [DotTemplateAdditionalActionsMenuComponent],
    imports: [MenuModule, ButtonModule, CommonModule],
    exports: [DotTemplateAdditionalActionsMenuComponent],
    providers: [DotMessageService]
})
export class DotTemplateAdditionalActionsMenuModule {}

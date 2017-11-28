import { NgModule } from '@angular/core';
import { MenuModule, ButtonModule } from 'primeng/primeng';
import { MessageService } from '../../../../../api/services/messages-service';
import { CommonModule } from '@angular/common';
import { DotTemplateAdditionalActionsMenuComponent } from './dot-template-additional-actions-menu.component';


@NgModule({
    declarations: [DotTemplateAdditionalActionsMenuComponent],
    imports: [MenuModule, ButtonModule, CommonModule],
    exports: [DotTemplateAdditionalActionsMenuComponent],
    providers: [MessageService]
})
export class DotTemplateAdditionalActionsMenuModule {}

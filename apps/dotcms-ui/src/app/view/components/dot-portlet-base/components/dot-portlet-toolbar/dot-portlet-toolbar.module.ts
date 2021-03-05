import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPortletToolbarComponent } from './dot-portlet-toolbar.component';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { MenuModule } from 'primeng/menu';

@NgModule({
    declarations: [DotPortletToolbarComponent],
    imports: [CommonModule, ToolbarModule, ButtonModule, DotMessagePipeModule, MenuModule],
    exports: [DotPortletToolbarComponent]
})
export class DotPortletToolbarModule {}

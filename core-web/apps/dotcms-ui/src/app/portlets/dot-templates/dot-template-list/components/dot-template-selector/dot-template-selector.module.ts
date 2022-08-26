import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateSelectorComponent } from './dot-template-selector.component';
import { DotIconModule } from '@dotcms/ui';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { AutoFocusModule } from 'primeng/autofocus';

@NgModule({
    declarations: [DotTemplateSelectorComponent],
    imports: [CommonModule, AutoFocusModule, DotIconModule, FormsModule, ButtonModule, DotMessagePipeModule]
})
export class DotTemplateSelectorModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateSelectorComponent } from './dot-template-selector.component';
import { DotIconModule } from '@dotcms/ui';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';

@NgModule({
    declarations: [DotTemplateSelectorComponent],
    imports: [CommonModule, DotAutofocusModule, DotIconModule, FormsModule, ButtonModule, DotMessagePipeModule]
})
export class DotTemplateSelectorModule {}

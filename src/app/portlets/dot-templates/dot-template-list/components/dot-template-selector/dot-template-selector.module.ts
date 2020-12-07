import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotTemplateSelectorComponent } from './dot-template-selector.component';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [DotTemplateSelectorComponent],
    imports: [CommonModule, DotIconModule, FormsModule, ButtonModule, DotMessagePipeModule]
})
export class DotTemplateSelectorModule {}

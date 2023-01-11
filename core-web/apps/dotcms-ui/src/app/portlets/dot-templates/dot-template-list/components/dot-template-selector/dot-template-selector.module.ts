import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';

import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotTemplateSelectorComponent } from './dot-template-selector.component';

@NgModule({
    declarations: [DotTemplateSelectorComponent],
    imports: [
        CommonModule,
        AutoFocusModule,
        DotIconModule,
        FormsModule,
        ButtonModule,
        DotMessagePipeModule
    ]
})
export class DotTemplateSelectorModule {}

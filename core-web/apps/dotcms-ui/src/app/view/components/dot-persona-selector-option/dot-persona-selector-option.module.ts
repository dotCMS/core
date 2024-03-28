import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';

import { DotAvatarDirective, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPersonaSelectorOptionComponent } from './dot-persona-selector-option.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        DotAvatarDirective,
        AvatarModule,
        BadgeModule,
        ButtonModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    declarations: [DotPersonaSelectorOptionComponent],
    exports: [DotPersonaSelectorOptionComponent]
})
export class DotPersonaSelectorOptionModule {}

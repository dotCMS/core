import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotAvatarDirective, DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        DotIconComponent,
        DotAvatarDirective,
        AvatarModule,
        BadgeModule,
        ButtonModule,
        TooltipModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    declarations: [DotPersonaSelectedItemComponent],
    exports: [DotPersonaSelectedItemComponent]
})
export class DotPersonaSelectedItemModule {}

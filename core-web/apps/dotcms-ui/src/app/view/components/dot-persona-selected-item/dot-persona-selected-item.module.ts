import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotAvatarDirective, DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        DotIconModule,
        DotAvatarDirective,
        AvatarModule,
        BadgeModule,
        ButtonModule,
        TooltipModule,
        DotPipesModule,
        DotMessagePipe
    ],
    declarations: [DotPersonaSelectedItemComponent],
    exports: [DotPersonaSelectedItemComponent]
})
export class DotPersonaSelectedItemModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';

import { DotAvatarDirective } from '@directives/dot-avatar/dot-avatar.directive';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPersonaSelectorOptionComponent } from './dot-persona-selector-option.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        DotAvatarDirective,
        AvatarModule,
        BadgeModule,
        ButtonModule,
        DotPipesModule,
        DotMessagePipe
    ],
    declarations: [DotPersonaSelectorOptionComponent],
    exports: [DotPersonaSelectorOptionComponent]
})
export class DotPersonaSelectorOptionModule {}

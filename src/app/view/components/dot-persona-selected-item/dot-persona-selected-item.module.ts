import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/primeng';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        DotIconModule,
        DotAvatarModule,
        ButtonModule,
        TooltipModule,
        DotPipesModule
    ],
    declarations: [DotPersonaSelectedItemComponent],
    exports: [DotPersonaSelectedItemComponent]
})
export class DotPersonaSelectedItemModule {}

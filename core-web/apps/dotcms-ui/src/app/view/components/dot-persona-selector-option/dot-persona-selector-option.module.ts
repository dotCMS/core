import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPersonaSelectorOptionComponent } from './dot-persona-selector-option.component';

@NgModule({
    imports: [CommonModule, FormsModule, DotAvatarModule, ButtonModule, DotPipesModule],
    declarations: [DotPersonaSelectorOptionComponent],
    exports: [DotPersonaSelectorOptionComponent]
})
export class DotPersonaSelectorOptionModule {}

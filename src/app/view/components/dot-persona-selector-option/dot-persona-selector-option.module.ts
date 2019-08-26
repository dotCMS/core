import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DotPersonaSelectorOptionComponent } from './dot-persona-selector-option.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { ButtonModule } from 'primeng/button';

@NgModule({
    imports: [CommonModule, FormsModule, DotAvatarModule, ButtonModule],
    declarations: [DotPersonaSelectorOptionComponent],
    exports: [DotPersonaSelectorOptionComponent]
})
export class DotPersonaSelectorOptionModule {}

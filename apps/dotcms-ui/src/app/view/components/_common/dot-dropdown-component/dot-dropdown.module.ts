import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DotDropdownComponent } from './dot-dropdown.component';
import { DotGravatarModule } from '../../dot-toolbar/components/dot-gravatar/dot-gravatar.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [CommonModule, ButtonModule, DotGravatarModule, DotIconButtonModule],
    declarations: [DotDropdownComponent],
    exports: [DotDropdownComponent]
})
export class DotDropdownModule {}

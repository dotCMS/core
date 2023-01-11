import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

import { DotDropdownComponent } from './dot-dropdown.component';

import { DotGravatarModule } from '../../dot-toolbar/components/dot-gravatar/dot-gravatar.module';


@NgModule({
    imports: [CommonModule, ButtonModule, DotGravatarModule, UiDotIconButtonModule],
    declarations: [DotDropdownComponent],
    exports: [DotDropdownComponent]
})
export class DotDropdownModule {}

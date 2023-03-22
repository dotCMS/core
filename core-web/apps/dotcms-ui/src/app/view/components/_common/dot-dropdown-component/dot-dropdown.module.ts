import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotGravatarComponent } from '@components/dot-toolbar/components/dot-gravatar/dot-gravatar.component';

import { DotDropdownComponent } from './dot-dropdown.component';

@NgModule({
    imports: [CommonModule, ButtonModule, DotGravatarComponent, UiDotIconButtonModule],
    declarations: [DotDropdownComponent],
    exports: [DotDropdownComponent]
})
export class DotDropdownModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { UiDotIconButtonModule } from '@dotcms/ui';

import { DotActionButtonComponent } from './dot-action-button.component';

@NgModule({
    declarations: [DotActionButtonComponent],
    exports: [DotActionButtonComponent],
    imports: [CommonModule, ButtonModule, MenuModule, UiDotIconButtonModule]
})
export class DotActionButtonModule {}

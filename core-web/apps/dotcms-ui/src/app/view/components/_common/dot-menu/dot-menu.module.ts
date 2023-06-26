import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { MenuModule } from 'primeng/menu';

import { UiDotIconButtonModule } from '@dotcms/ui';

import { DotMenuComponent } from './dot-menu.component';

@NgModule({
    declarations: [DotMenuComponent],
    exports: [DotMenuComponent],
    imports: [CommonModule, UiDotIconButtonModule, MenuModule]
})
export class DotMenuModule {}

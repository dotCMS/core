import { NgModule } from '@angular/core';
import { DotMenuComponent } from './dot-menu.component';
import { CommonModule } from '@angular/common';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { MenuModule } from 'primeng/menu';

@NgModule({
    declarations: [DotMenuComponent],
    exports: [DotMenuComponent],
    imports: [CommonModule, UiDotIconButtonModule, MenuModule]
})
export class DotMenuModule {}

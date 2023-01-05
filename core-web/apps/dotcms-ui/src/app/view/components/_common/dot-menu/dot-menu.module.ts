import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { MenuModule } from 'primeng/menu';
import { DotMenuComponent } from './dot-menu.component';

@NgModule({
    declarations: [DotMenuComponent],
    exports: [DotMenuComponent],
    imports: [CommonModule, UiDotIconButtonModule, MenuModule]
})
export class DotMenuModule {}

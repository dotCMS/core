import { NgModule } from '@angular/core';
import { DotMenuComponent } from './dot-menu.component';
import { CommonModule } from '@angular/common';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    declarations: [DotMenuComponent],
    exports: [DotMenuComponent],
    imports: [CommonModule, DotIconButtonModule]
})
export class DotMenuModule {}

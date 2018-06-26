import { NgModule } from '@angular/core';
import { DotMenuComponent } from './dot-menu.component';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';

@NgModule({
    declarations: [DotMenuComponent],
    exports: [DotMenuComponent],
    imports: [CommonModule, ButtonModule]
})
export class DotMenuModule {}

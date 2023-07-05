import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotMenuComponent } from './dot-menu.component';

@NgModule({
    declarations: [DotMenuComponent],
    exports: [DotMenuComponent],
    imports: [CommonModule, ButtonModule, MenuModule, BrowserAnimationsModule]
})
export class DotMenuModule {}

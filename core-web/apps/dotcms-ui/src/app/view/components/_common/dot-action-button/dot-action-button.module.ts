import { DotActionButtonComponent } from './dot-action-button.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { UiDotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

@NgModule({
    declarations: [DotActionButtonComponent],
    exports: [DotActionButtonComponent],
    imports: [CommonModule, ButtonModule, MenuModule, UiDotIconButtonModule]
})
export class DotActionButtonModule {}

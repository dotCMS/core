import { DotActionButtonComponent } from './dot-action-button.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ButtonModule, MenuModule } from 'primeng/primeng';
import { DotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';

@NgModule({
    declarations: [DotActionButtonComponent],
    exports: [DotActionButtonComponent],
    imports: [CommonModule, ButtonModule, MenuModule, DotIconButtonModule ]
})
export class DotActionButtonModule {}

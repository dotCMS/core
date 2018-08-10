import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIconModule } from '../dot-icon/dot-icon.module';
import { DotIconButtonComponent } from './dot-icon-button.component';

@NgModule({
    declarations: [DotIconButtonComponent],
    exports: [DotIconButtonComponent],
    imports: [CommonModule, DotIconModule]
})
export class DotIconButtonModule {}

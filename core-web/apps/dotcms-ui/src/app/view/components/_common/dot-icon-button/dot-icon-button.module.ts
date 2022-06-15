import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotIconModule } from '@dotcms/ui';
import { UiDotIconButtonComponent } from './dot-icon-button.component';

@NgModule({
    declarations: [UiDotIconButtonComponent],
    exports: [UiDotIconButtonComponent],
    imports: [CommonModule, DotIconModule]
})
export class UiDotIconButtonModule {}

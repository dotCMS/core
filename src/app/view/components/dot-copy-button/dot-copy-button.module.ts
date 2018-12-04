import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotCopyButtonComponent } from './dot-copy-button.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [CommonModule, DotIconButtonModule],
    declarations: [DotCopyButtonComponent],
    exports: [DotCopyButtonComponent]
})
export class DotCopyButtonModule {}

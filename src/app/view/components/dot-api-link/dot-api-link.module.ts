import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotApiLinkComponent } from './dot-api-link.component';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

@NgModule({
    imports: [CommonModule, DotIconModule],
    declarations: [DotApiLinkComponent],
    exports: [DotApiLinkComponent]
})
export class DotApiLinkModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotRelativeDatePipe } from '@dotcms/ui';

import { CustomTimeComponent } from './dot-custom-time.component';

@NgModule({
    imports: [CommonModule, DotRelativeDatePipe],
    exports: [CustomTimeComponent],
    declarations: [CustomTimeComponent]
})
export class DotCustomTimeModule {}

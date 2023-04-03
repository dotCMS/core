import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotRelativeDatePipe } from '@dotcms/app/view/pipes/dot-relative-date/dot-relative-date.pipe';

import { CustomTimeComponent } from './dot-custom-time.component';

@NgModule({
    imports: [CommonModule, DotRelativeDatePipe],
    exports: [CustomTimeComponent],
    declarations: [CustomTimeComponent]
})
export class DotCustomTimeModule {}

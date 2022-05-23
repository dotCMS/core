import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CustomTimeComponent } from './dot-custom-time.component';

@NgModule({
    imports: [CommonModule],
    exports: [CustomTimeComponent],
    declarations: [CustomTimeComponent]
})
export class DotCustomTimeModule {}

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotPortletBoxComponent } from './dot-portlet-box.component';

@NgModule({
    imports: [CommonModule],
    declarations: [DotPortletBoxComponent],
    exports: [DotPortletBoxComponent]
})
export class DotPortletBoxModule {}

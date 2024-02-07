import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ToastModule } from 'primeng/toast';

import { DotMessageDisplayService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';

import { DotMessageDisplayComponent } from './dot-message-display.component';

@NgModule({
    imports: [CommonModule, ToastModule, DotIconModule],
    declarations: [DotMessageDisplayComponent],
    providers: [DotMessageDisplayService],
    exports: [DotMessageDisplayComponent]
})
export class DotMessageDisplayModule {}

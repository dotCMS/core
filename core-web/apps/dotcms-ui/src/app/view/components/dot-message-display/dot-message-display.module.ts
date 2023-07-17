import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ToastModule } from 'primeng/toast';

import { DotIconModule, UiDotIconButtonModule } from '@dotcms/ui';

import { DotMessageDisplayComponent } from './dot-message-display.component';
import { DotMessageDisplayService } from './services';

@NgModule({
    imports: [CommonModule, ToastModule, DotIconModule, UiDotIconButtonModule],
    declarations: [DotMessageDisplayComponent],
    providers: [DotMessageDisplayService],
    exports: [DotMessageDisplayComponent]
})
export class DotMessageDisplayModule {}

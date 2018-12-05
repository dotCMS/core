import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessageDisplayComponent } from './dot-message-display.component';
import { DotMessageDisplayService } from './services/dot-message-display.service';
import {ToastModule} from 'primeng/toast';
import { DotRouterService } from 'dotcms-js';

@NgModule({
    imports: [CommonModule, ToastModule],
    declarations: [DotMessageDisplayComponent],
    providers: [DotMessageDisplayService],
    exports: [DotMessageDisplayComponent]
})
export class DotMessageDisplayModule {}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessageDisplayComponent } from './dot-message-display.component';
import { DotMessageDisplayService } from './services';
import {ToastModule} from 'primeng/toast';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

@NgModule({
    imports: [CommonModule, ToastModule, DotIconModule],
    declarations: [DotMessageDisplayComponent],
    providers: [DotMessageDisplayService],
    exports: [DotMessageDisplayComponent]
})
export class DotMessageDisplayModule {}

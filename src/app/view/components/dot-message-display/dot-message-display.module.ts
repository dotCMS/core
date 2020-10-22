import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessageDisplayComponent } from './dot-message-display.component';
import { DotMessageDisplayService } from './services';
import { ToastModule } from 'primeng/toast';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [CommonModule, ToastModule, DotIconModule, DotIconButtonModule],
    declarations: [DotMessageDisplayComponent],
    providers: [DotMessageDisplayService],
    exports: [DotMessageDisplayComponent]
})
export class DotMessageDisplayModule {}

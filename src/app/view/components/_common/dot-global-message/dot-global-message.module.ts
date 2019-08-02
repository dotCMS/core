import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotGlobalMessageService } from './dot-global-message.service';
import { DotGlobalMessageComponent } from './dot-global-message.component';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotSpinnerModule } from '@components/_common/dot-spinner/dot-spinner.module';

@NgModule({
    imports: [CommonModule, DotIconModule, DotSpinnerModule],
    declarations: [DotGlobalMessageComponent],
    exports: [DotGlobalMessageComponent],
    providers: [DotGlobalMessageService]
})
export class DotGlobalMessageModule {}

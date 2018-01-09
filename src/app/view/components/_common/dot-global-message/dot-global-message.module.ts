import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotGlobalMessageService } from './dot-global-message.service';
import { DotGlobalMessageComponent } from './dot-global-message.component';

@NgModule({
    imports: [CommonModule],
    declarations: [DotGlobalMessageComponent],
    exports: [DotGlobalMessageComponent],
    providers: [DotGlobalMessageService]
})
export class DotGlobalMessageModule {}

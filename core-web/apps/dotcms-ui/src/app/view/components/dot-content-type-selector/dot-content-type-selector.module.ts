import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { DotMessagePipeModule } from '../../pipes/dot-message/dot-message-pipe.module';

import { DotContentTypeSelectorComponent } from './dot-content-type-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule, DotMessagePipeModule],
    declarations: [DotContentTypeSelectorComponent],
    exports: [DotContentTypeSelectorComponent]
})
export class DotContentTypeSelectorModule {}

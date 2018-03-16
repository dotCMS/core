import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotDeviceSelectorComponent } from './dot-device-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule],
    declarations: [DotDeviceSelectorComponent],
    exports: [DotDeviceSelectorComponent]
})
export class DotDeviceSelectorModule {}

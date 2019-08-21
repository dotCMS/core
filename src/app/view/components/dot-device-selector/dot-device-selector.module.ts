import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotDeviceSelectorComponent } from './dot-device-selector.component';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule, DotIconModule],
    declarations: [DotDeviceSelectorComponent],
    exports: [DotDeviceSelectorComponent]
})
export class DotDeviceSelectorModule {}

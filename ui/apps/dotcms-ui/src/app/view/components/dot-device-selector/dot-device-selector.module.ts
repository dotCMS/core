import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';
import { DotDeviceSelectorComponent } from './dot-device-selector.component';
import { DotIconModule } from '@dotcms/ui';
import { DotDevicesService } from '@services/dot-devices/dot-devices.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule, DotIconModule, DotPipesModule],
    declarations: [DotDeviceSelectorComponent],
    exports: [DotDeviceSelectorComponent],
    providers: [DotDevicesService]
})
export class DotDeviceSelectorModule {}

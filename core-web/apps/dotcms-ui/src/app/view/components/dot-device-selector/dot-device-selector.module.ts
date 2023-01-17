import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotDevicesService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotDeviceSelectorComponent } from './dot-device-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule, DotIconModule, DotPipesModule],
    declarations: [DotDeviceSelectorComponent],
    exports: [DotDeviceSelectorComponent],
    providers: [DotDevicesService]
})
export class DotDeviceSelectorModule {}

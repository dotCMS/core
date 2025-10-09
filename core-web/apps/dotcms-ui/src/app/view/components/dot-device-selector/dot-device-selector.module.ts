import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotDevicesService } from '@dotcms/data-access';
import { DotIconComponent, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotDeviceSelectorComponent } from './dot-device-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule, DotIconComponent, DotSafeHtmlPipe],
    declarations: [DotDeviceSelectorComponent],
    exports: [DotDeviceSelectorComponent],
    providers: [DotDevicesService]
})
export class DotDeviceSelectorModule {}

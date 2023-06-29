import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';

import { DotDevicesService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotDeviceSelectorSeoComponent } from './dot-device-selector-seo.component';

@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        DotIconModule,
        DotPipesModule,
        ButtonModule,
        OverlayPanelModule,
        PanelModule
    ],
    declarations: [DotDeviceSelectorSeoComponent],
    exports: [DotDeviceSelectorSeoComponent],
    providers: [DotDevicesService]
})
export class DotSiteSelectorSeoModule {}

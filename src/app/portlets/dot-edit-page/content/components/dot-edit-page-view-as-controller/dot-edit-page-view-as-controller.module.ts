import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageViewAsControllerComponent } from './dot-edit-page-view-as-controller.component';
import { DropdownModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotLanguageSelectorModule } from '@components/dot-language-selector/dot-language-selector.module';
import { DotDeviceSelectorModule } from '@components/dot-device-selector/dot-device-selector.module';
import { DotPersonasService } from '@services/dot-personas/dot-personas.service';
import { DotLanguagesService } from '@services/dot-languages/dot-languages.service';
import { DotDevicesService } from '@services/dot-devices/dot-devices.service';
import { DotPersonaSelectorModule } from '@components/dot-persona-selector/dot-persona.selector.module';


@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        DotPersonaSelectorModule,
        DotLanguageSelectorModule,
        DotDeviceSelectorModule
    ],
    providers: [DotDevicesService, DotLanguagesService, DotPersonasService],
    declarations: [DotEditPageViewAsControllerComponent],
    exports: [DotEditPageViewAsControllerComponent]
})
export class DotEditPageViewAsControllerModule {}

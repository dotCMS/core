import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditContentViewAsToolbarComponent } from './dot-edit-content-view-as-toolbar.component';
import { DropdownModule, CheckboxModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotPersonaSelectorModule } from '@components/dot-persona-selector/dot-persona-selector.module';
import { DotLanguageSelectorModule } from '@components/dot-language-selector/dot-language-selector.module';
import { DotDeviceSelectorModule } from '@components/dot-device-selector/dot-device-selector.module';
import { DotPersonasService } from '@services/dot-personas/dot-personas.service';
import { DotLanguagesService } from '@services/dot-languages/dot-languages.service';
import { DotDevicesService } from '@services/dot-devices/dot-devices.service';

@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        DotPersonaSelectorModule,
        DotLanguageSelectorModule,
        DotDeviceSelectorModule,
        CheckboxModule
    ],
    providers: [DotDevicesService, DotLanguagesService, DotPersonasService],
    declarations: [DotEditContentViewAsToolbarComponent],
    exports: [DotEditContentViewAsToolbarComponent]
})
export class DotEditContentViewAsToolbarModule {}

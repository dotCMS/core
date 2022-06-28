import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageViewAsControllerComponent } from './dot-edit-page-view-as-controller.component';
import { FormsModule } from '@angular/forms';
import { DotLanguageSelectorModule } from '@components/dot-language-selector/dot-language-selector.module';
import { DotDeviceSelectorModule } from '@components/dot-device-selector/dot-device-selector.module';
import { DotPersonaSelectorModule } from '@components/dot-persona-selector/dot-persona.selector.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';

@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        TooltipModule,
        DotPersonaSelectorModule,
        DotLanguageSelectorModule,
        DotDeviceSelectorModule,
        DotPipesModule
    ],
    declarations: [DotEditPageViewAsControllerComponent],
    exports: [DotEditPageViewAsControllerComponent]
})
export class DotEditPageViewAsControllerModule {}

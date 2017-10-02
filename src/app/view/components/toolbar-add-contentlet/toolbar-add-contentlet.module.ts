import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToolbarAddContenletComponent } from './toolbar-add-contentlet.component';
import { ToolbarAddContenletBodyComponent } from './toolbar-add-contentlet-body.component';
import { ToolbarAddContenletService } from './toolbar-add-contentlet.service';
import { DotDropdownModule } from '../_common/dropdown-component/dot-dropdown.module';
import { ButtonModule } from 'primeng/primeng';
import { ActionButtonModule } from '../_common/action-button/action-button.module';
import { RouterModule } from '@angular/router';
import { DotContentletService } from '../../../api/services/dot-contentlet.service';

@NgModule({
    declarations: [ToolbarAddContenletComponent, ToolbarAddContenletBodyComponent],
    imports: [CommonModule, DotDropdownModule, ButtonModule, ActionButtonModule, RouterModule],
    exports: [ToolbarAddContenletComponent, ToolbarAddContenletBodyComponent],
    providers: [ToolbarAddContenletService, DotContentletService]
})
export class ToolbarAddContenletModule {}

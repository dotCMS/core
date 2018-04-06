import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageNavComponent } from './dot-edit-page-nav.component';
import { RouterModule } from '@angular/router';
import { TooltipModule } from 'primeng/primeng';
import { DotLicenseService } from '../../../../api/services/dot-license/dot-license.service';

@NgModule({
    imports: [CommonModule, RouterModule, TooltipModule],
    declarations: [DotEditPageNavComponent],
    exports: [DotEditPageNavComponent],
    providers: [DotLicenseService]
})
export class DotEditPageNavModule {}

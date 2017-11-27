import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutGridModule } from '../dot-edit-layout-grid/dot-edit-layout-grid.module';

@NgModule({
    declarations: [DotEditLayoutComponent],
    imports: [CommonModule, DotEditLayoutGridModule],
    exports: [DotEditLayoutComponent],
    providers: [DotEditLayoutService]
})
export class DotEditLayoutModule {}

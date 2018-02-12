import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotEditLayoutAdvancedComponent } from './dot-edit-layout-advanced.component';
import { IFrameModule } from '../../../../view/components/_common/iframe';

@NgModule({
    imports: [CommonModule, IFrameModule],
    exports: [DotEditLayoutAdvancedComponent],
    declarations: [DotEditLayoutAdvancedComponent],
    providers: []
})
export class DotEditLayoutAdvancedModule { }

import { NgModule } from '@angular/core';

import { DotContentletsComponent } from './dot-contentlets.component';

import { DotEditContentletComponent } from '../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';

@NgModule({
    declarations: [DotContentletsComponent],
    imports: [DotEditContentletComponent],
    exports: [DotContentletsComponent],
    providers: []
})
export class DotContentletsModule {}

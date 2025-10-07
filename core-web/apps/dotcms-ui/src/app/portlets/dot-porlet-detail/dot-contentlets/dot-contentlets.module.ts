import { NgModule } from '@angular/core';

import { DotContentletsComponent } from './dot-contentlets.component';

import { DotContentletEditorModule } from '../../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';

@NgModule({
    declarations: [DotContentletsComponent],
    imports: [DotContentletEditorModule],
    exports: [DotContentletsComponent],
    providers: []
})
export class DotContentletsModule {}

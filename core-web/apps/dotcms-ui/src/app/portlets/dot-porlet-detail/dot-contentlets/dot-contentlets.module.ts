import { NgModule } from '@angular/core';

import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';

import { DotContentletsComponent } from './dot-contentlets.component';

@NgModule({
    declarations: [DotContentletsComponent],
    imports: [DotContentletEditorModule],
    exports: [DotContentletsComponent],
    providers: []
})
export class DotContentletsModule {}

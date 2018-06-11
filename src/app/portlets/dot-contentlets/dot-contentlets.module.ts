import { DotContentletsComponent } from './dot-contentlets.component';
import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { DotContentletEditorModule } from '../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';

const routes: Routes = [
    {
        component: DotContentletsComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotContentletsComponent],
    imports: [
        DotContentletEditorModule,
        RouterModule.forChild(routes),
    ],
    exports: [],
    providers: []
})
export class DotContentletsModule {}

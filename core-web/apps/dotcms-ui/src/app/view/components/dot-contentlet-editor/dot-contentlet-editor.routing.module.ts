import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotCreateContentletComponent } from './components/dot-create-contentlet/dot-create-contentlet.component';
import { DotCreateContentletResolver } from './components/dot-create-contentlet/dot-create-contentlet.resolver.service';

const routes: Routes = [
    {
        component: DotCreateContentletComponent,
        path: ':contentType',
        resolve: {
            url: DotCreateContentletResolver
        }
    },
    {
        path: '',
        redirectTo: '/c/content'
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotContentletEditorRoutingModule {}

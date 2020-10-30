import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotTemplateListComponent } from '@portlets/dot-templates/dot-template-list/dot-template-list.component';
import { DotTemplateListResolver } from '@portlets/dot-templates/dot-template-list/dot-template-list-resolver.service';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateListComponent,
        resolve: {
            dotTemplateListResolverData: DotTemplateListResolver
        }
    }
];

@NgModule({ imports: [RouterModule.forChild(routes)] })
export class DotTemplatesRoutingModule {}

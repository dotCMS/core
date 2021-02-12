import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotTemplateListComponent } from '@portlets/dot-templates/dot-template-list/dot-template-list.component';
import { DotTemplateListResolver } from '@portlets/dot-templates/dot-template-list/dot-template-list-resolver.service';
import { DotTemplateCreateEditResolver } from './dot-template-create-edit/resolvers/dot-template-create-edit.resolver';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateListComponent,
        resolve: {
            dotTemplateListResolverData: DotTemplateListResolver
        },
        data: {
            reuseRoute: false
        }
    },
    {
        path: 'new',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-create-edit/dot-template-new/dot-template-new.module.ts'
            ).then((m) => m.DotTemplateNewModule)
    },
    {
        path: 'edit/:id',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-create-edit/dot-template-create-edit.module.ts'
            ).then((m) => m.DotTemplateCreateEditModule),
        resolve: {
            template: DotTemplateCreateEditResolver
        }
    },
    {
        path: 'edit/:id/inode/:inode',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-create-edit/dot-template-create-edit.module.ts'
            ).then((m) => m.DotTemplateCreateEditModule),
        data: {
            reuseRoute: false
        },
        resolve: {
            template: DotTemplateCreateEditResolver
        }
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplatesRoutingModule {}

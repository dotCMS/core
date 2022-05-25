import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotTemplateListComponent } from './dot-template-list/dot-template-list.component';
import { DotTemplateListResolver } from './dot-template-list/dot-template-list-resolver.service';
import { DotTemplateCreateEditResolver } from './dot-template-create-edit/resolvers/dot-template-create-edit.resolver';
import { LayoutEditorCanDeactivateGuardService } from '@services/guards/layout-editor-can-deactivate-guard.service';

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
                '@portlets/dot-templates/dot-template-create-edit/dot-template-new/dot-template-new.module'
            ).then((m) => m.DotTemplateNewModule)
    },
    {
        path: 'edit/:id',
        canDeactivate: [LayoutEditorCanDeactivateGuardService],
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-create-edit/dot-template-create-edit.module'
            ).then((m) => m.DotTemplateCreateEditModule),
        resolve: {
            template: DotTemplateCreateEditResolver
        }
    },
    {
        path: 'edit/:id/inode/:inode',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-create-edit/dot-template-create-edit.module'
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

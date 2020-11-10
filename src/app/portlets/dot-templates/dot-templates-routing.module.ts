import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotTemplateDesignerResolver } from './dot-template-designer/dot-template-designer.resolver';
import { DotTemplateListComponent } from '@portlets/dot-templates/dot-template-list/dot-template-list.component';
import { DotTemplateListResolver } from '@portlets/dot-templates/dot-template-list/dot-template-list-resolver.service';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateListComponent,
        resolve: {
            dotTemplateListResolverData: DotTemplateListResolver
        }
    },
    {
        path: 'new',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-designer/dot-template-designer.module.ts'
            ).then((m) => m.DotTemplateDesignerModule)
    },
    {
        path: 'new/advanced',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-advanced/dot-template-advanced.module'
            ).then((m) => m.DotTemplateModule)
    },
    {
        path: ':inode',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-designer/dot-template-designer.module.ts'
            ).then((m) => m.DotTemplateDesignerModule),
        resolve: {
            template: DotTemplateDesignerResolver
        }
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
    providers: [DotTemplateDesignerResolver]
})
export class DotTemplatesRoutingModule {}

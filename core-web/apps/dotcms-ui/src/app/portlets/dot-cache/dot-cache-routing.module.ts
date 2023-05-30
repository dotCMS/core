import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotCacheListResolver } from '@portlets/dot-cache/dot-cache-list/dot-cache-list-resolver.service';
import { DotCacheListComponent } from '@portlets/dot-cache/dot-cache-list/dot-cache-list.component';

const routes: Routes = [
    {
        path: '',
        component: DotCacheListComponent,
        resolve: {
            dotCacheListResolverData: DotCacheListResolver
        },
        data: {
            reuseRoute: false
        }
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)]
})
export class DotCacheRoutingModule {}

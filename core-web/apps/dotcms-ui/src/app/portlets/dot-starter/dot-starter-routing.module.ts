import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterComponent } from './dot-starter.component';

const routes: Routes = [
    {
        component: DotStarterComponent,
        path: '',
        resolve: {
            userData: DotStarterResolver
        }
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotStarterRoutingModule {}

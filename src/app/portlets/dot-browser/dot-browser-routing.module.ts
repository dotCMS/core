import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoutingPrivateAuthService } from '../../api/services/routing-private-auth-service';
import { DotBrowserComponent } from './dot-browser-component';

const dotBrowserRoutes: Routes = [
    {
        canActivate: [RoutingPrivateAuthService],
        component: DotBrowserComponent,
        path: '',
    },
];

@NgModule({
    exports: [
        RouterModule
    ],
    imports: [
        RouterModule.forChild(dotBrowserRoutes)
    ]
})
export class DotBrowserRoutingModule { }

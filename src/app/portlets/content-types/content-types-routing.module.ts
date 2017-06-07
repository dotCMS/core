import { Component, OnInit } from '@angular/core';
import { ContentTypesCreateEditPortletComponent } from './create-edit/main';
import { ContentTypesPortletComponent } from './listing';
import { MainComponentLegacy } from '../../view/components/main-legacy/main-legacy-component';
import { NgModule } from '@angular/core';
import { PatternLibrary } from '../../view/components/_common/pattern-library/pattern-library';
import { RouterModule, Routes } from '@angular/router';
import { RoutingPrivateAuthService } from '../../api/services/routing-private-auth-service';

const contentTypesRoutes: Routes = [
    {
        canActivate: [RoutingPrivateAuthService],
        component: ContentTypesPortletComponent,
        path: '',
    },
    {
        canActivate: [RoutingPrivateAuthService],
        component: ContentTypesCreateEditPortletComponent,
        path: 'create/:id'
    }
];

@NgModule({
    exports: [
        RouterModule
    ],
    imports: [
        RouterModule.forChild(contentTypesRoutes)
    ]
})
export class ContentTypesRoutingModule { }
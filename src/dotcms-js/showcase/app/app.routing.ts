import {AppComponent} from './app.component';
import {Routes, RouterModule} from '@angular/router';
import {ModuleWithProviders} from '@angular/core';
import {IntroDemoShowcase} from "../components/intro/intro";
import {SiteSelectorDemoShowcase} from "../components/site-selector/site-selector";
import {BreadcrumbDemoShowcase} from "../components/breadcrumb/breadcrumb";
import {SiteTreeTableDemoShowcase} from "../components/site-treetable/site-treetable";
import {SiteDatatableDemoShowcase} from "../components/site-datatable/site-datatable";
import {TreeableDetailComponentDemoShowcase} from "../components/treeable-detail/treeable-detail";

const appRoutes: Routes = [
    {
        path: '',
        component: IntroDemoShowcase,
    },
    {
        path: 'breadcrumb',
        component: BreadcrumbDemoShowcase
    },
    {
        path: 'site-datatable',
        component: SiteDatatableDemoShowcase
    },
    {
        path: 'site-selector',
        component: SiteSelectorDemoShowcase
    },
    {
        path: 'site-treetable',
        component: SiteTreeTableDemoShowcase
    },
    {
        path: 'treable-detail',
        component: TreeableDetailComponentDemoShowcase
    },

];


export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);
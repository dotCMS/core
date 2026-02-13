import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ApiRoot } from '@dotcms/dotcms-js';
import { portletHaveLicenseResolver } from '@dotcms/ui';

import { AppRulesComponent } from './app.component';
import { RuleEngineModule } from './rule-engine.module';

const routes: Routes = [
    {
        component: AppRulesComponent,
        path: '',
        resolve: { haveLicense: portletHaveLicenseResolver }
    }
];

@NgModule({
    imports: [RuleEngineModule, RouterModule.forChild(routes)],
    declarations: [],
    providers: [ApiRoot],
    exports: [AppRulesComponent]
})
export class DotRulesModule {}

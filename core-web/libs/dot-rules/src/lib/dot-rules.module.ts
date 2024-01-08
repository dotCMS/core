import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AppRulesComponent, RuleEngineModule } from '@dotcms/dot-rules';
import { ApiRoot } from '@dotcms/dotcms-js';
import { rulesResolver } from './dot-rules.resolver';

const routes: Routes = [
    {
        component: AppRulesComponent,
        path: '',
        resolve: { haveLicense: rulesResolver }
    }
];

@NgModule({
    imports: [RuleEngineModule, RouterModule.forChild(routes)],
    declarations: [],
    providers: [ApiRoot],
    exports: [AppRulesComponent]
})
export class DotRulesModule {}

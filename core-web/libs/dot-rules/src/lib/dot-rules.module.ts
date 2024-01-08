import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AppRulesComponent, RuleEngineModule } from '@dotcms/dot-rules';
import { ApiRoot } from '@dotcms/dotcms-js';
import { portletHaveLicenseResolver } from '@dotcms/ui';

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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ApiRoot } from '@dotcms/dotcms-js';

import { DotRulesComponent } from './app/dot-rules.component';
import { RuleEngineModule } from './rule-engine.module';

const routes: Routes = [
    {
        component: DotRulesComponent,
        path: ''
    }
];

@NgModule({
    imports: [RuleEngineModule, RouterModule.forChild(routes)],
    declarations: [],
    providers: [ApiRoot],
    exports: [DotRulesComponent]
})
export class DotRulesModule {}

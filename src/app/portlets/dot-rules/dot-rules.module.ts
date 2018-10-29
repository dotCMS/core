import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { RuleEngineModule } from 'dot-rules';

import { AppRulesComponent } from 'dot-rules';

const routes: Routes = [
    {
        component: AppRulesComponent,
        path: ''
    }
];

@NgModule({
    imports: [RuleEngineModule, RouterModule.forChild(routes)],
    declarations: [],
    providers: [],
    exports: [AppRulesComponent]
})
export class DotRulesModule {}

import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { RuleEngineModule } from '@dotcms/dot-rules';
import { AppRulesComponent } from '@dotcms/dot-rules';
import { ApiRoot } from '@dotcms/dotcms-js'

const routes: Routes = [
    {
        component: AppRulesComponent,
        path: ''
    }
];

@NgModule({
  imports: [RuleEngineModule, RouterModule.forChild(routes)],
  declarations: [],
  providers: [ApiRoot],
  exports: [AppRulesComponent],
})
export class DotRulesModule {}

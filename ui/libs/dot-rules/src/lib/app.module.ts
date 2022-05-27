import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { RuleEngineModule } from './rule-engine.module';

@NgModule({
    declarations: [],
    imports: [
        BrowserModule,
        RuleEngineModule,
        RouterModule.forRoot([], {
            useHash: true
        })
    ],
    providers: [],
    exports: [],
    bootstrap: []
})
export class AppRulesModule {}

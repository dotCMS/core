import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { AppRulesComponent } from './app.component';
import { RuleEngineModule } from './rule-engine.module';

@NgModule({
    declarations: [AppRulesComponent],
    imports: [
        BrowserModule,
        RuleEngineModule,
        RouterModule.forRoot([], {
            useHash: true
        })
    ],
    providers: [],
    bootstrap: [AppRulesComponent]
})
export class AppModule {}

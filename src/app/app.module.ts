import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule, JsonpModule } from '@angular/http';
import { NgModule } from '@angular/core';

// App is our top level component
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';

/*
 * Custom Components
 */
import { COMPONENTS } from './components';
import { ENV_PROVIDERS } from './providers';
import { CUSTOM_MODULES, NGFACES_MODULES } from './modules';
import { SharedModule } from './shared/shared.module';

import { HotkeyModule } from 'angular2-hotkeys';
import { DotDirectivesModule } from './shared/dot-directives.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

/**
 * `AppModule` is the main entry point into Angular2's bootstraping process
 */
@NgModule({
    bootstrap: [AppComponent],
    declarations: [AppComponent, ...COMPONENTS],
    imports: [
        ...CUSTOM_MODULES,
        ...NGFACES_MODULES,
        BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        HttpModule,
        JsonpModule,
        ReactiveFormsModule,
        SharedModule.forRoot(),
        AppRoutingModule,
        MdInputTextModule,
        DotDirectivesModule,
        DotPipesModule,
        HotkeyModule.forRoot()
    ],
    providers: [ENV_PROVIDERS]
})
export class AppModule {}

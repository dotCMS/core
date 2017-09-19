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
import { COMPONENTS, PIPES } from './components';
import { ENV_PROVIDERS } from './providers';
import { CUSTOM_MODULES, NGFACES_MODULES } from './modules';
import { SharedModule } from './shared/shared.module';

/**
 * `AppModule` is the main entry point into Angular2's bootstraping process
 */
@NgModule({
    bootstrap: [AppComponent],
    declarations: [AppComponent, ...PIPES, ...COMPONENTS],
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
        AppRoutingModule
    ],
    providers: [
        ENV_PROVIDERS
    ]
})
export class AppModule {}
